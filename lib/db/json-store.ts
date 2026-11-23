import { promises as fs } from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { DEMO_DELIVERIES } from '../demo';
import type { Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus } from '../types';
import type { DatabaseHealth, DeliveryRepository } from './repository';

interface JsonState {
  version: 1;
  nextOrderNo: number;
  seeded: boolean;
  deliveries: Delivery[];
}

const EMPTY_STATE: JsonState = { version: 1, nextOrderNo: 1, seeded: false, deliveries: [] };

let queue: Promise<unknown> = Promise.resolve();

/** Serialises all reads/writes so concurrent API calls cannot corrupt the file. */
function withLock<T>(task: () => Promise<T>): Promise<T> {
  const run = queue.then(task, task);
  queue = run.then(
    () => undefined,
    () => undefined,
  );
  return run;
}

function dataDir(): string {
  if (process.env.DATA_DIR) return process.env.DATA_DIR;
  // Serverless filesystems are read-only except /tmp.
  if (process.env.VERCEL) return '/tmp/yousif-delivery';
  return path.join(process.cwd(), '.data');
}

function storeFile(): string {
  return path.join(dataDir(), 'store.json');
}

async function readState(): Promise<JsonState> {
  try {
    const raw = await fs.readFile(storeFile(), 'utf8');
    const parsed = JSON.parse(raw) as Partial<JsonState>;
    return {
      version: 1,
      nextOrderNo: typeof parsed.nextOrderNo === 'number' ? parsed.nextOrderNo : 1,
      seeded: Boolean(parsed.seeded),
      deliveries: Array.isArray(parsed.deliveries) ? (parsed.deliveries as Delivery[]) : [],
    };
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    if (code === 'ENOENT') {
      const seeded = buildSeedState();
      await writeState(seeded);
      return seeded;
    }
    throw error;
  }
}

async function writeState(state: JsonState): Promise<void> {
  const file = storeFile();
  await fs.mkdir(path.dirname(file), { recursive: true });
  const tmp = `${file}.${process.pid}.tmp`;
  await fs.writeFile(tmp, JSON.stringify(state, null, 2), 'utf8');
  await fs.rename(tmp, file);
}

function buildSeedState(): JsonState {
  const now = Date.now();
  let orderNo = 1;
  const deliveries: Delivery[] = DEMO_DELIVERIES.map((demo) => {
    const createdAt = new Date(now - demo.minutesAgo * 60_000);
    const deliveredAt =
      demo.status === 'delivered' ? new Date(createdAt.getTime() + 18 * 60_000) : null;
    const delivery: Delivery = {
      id: randomUUID(),
      orderNo: orderNo++,
      customerId: randomUUID(),
      name: demo.name,
      primaryPhone: demo.primaryPhone,
      extraPhones: [...demo.extraPhones],
      latitude: demo.latitude,
      longitude: demo.longitude,
      address: demo.address,
      landmark: demo.landmark,
      channels: [...demo.channels],
      customerNotes: demo.customerNotes,
      itemNote: demo.itemNote,
      price: demo.price,
      currency: demo.currency,
      deliveryFee: demo.deliveryFee,
      status: demo.status,
      createdAt: createdAt.toISOString(),
      updatedAt: createdAt.toISOString(),
      deliveredAt: deliveredAt ? deliveredAt.toISOString() : null,
    };
    return delivery;
  });

  return { version: 1, nextOrderNo: orderNo, seeded: true, deliveries };
}

function sortDeliveries(deliveries: Delivery[]): Delivery[] {
  return [...deliveries].sort((a, b) => {
    const delta = new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    if (delta !== 0) return delta;
    return b.orderNo - a.orderNo;
  });
}

function applyPatch(delivery: Delivery, patch: DeliveryPatch): Delivery {
  const next: Delivery = {
    ...delivery,
    updatedAt: new Date().toISOString(),
    name: patch.name ?? delivery.name,
    primaryPhone: patch.primaryPhone ?? delivery.primaryPhone,
    extraPhones: patch.extraPhones ?? delivery.extraPhones,
    latitude: patch.latitude !== undefined ? patch.latitude : delivery.latitude,
    longitude: patch.longitude !== undefined ? patch.longitude : delivery.longitude,
    address: patch.address ?? delivery.address,
    landmark: patch.landmark ?? delivery.landmark,
    channels: patch.channels ?? delivery.channels,
    customerNotes: patch.customerNotes ?? delivery.customerNotes,
    itemNote: patch.itemNote ?? delivery.itemNote,
    price: patch.price ?? delivery.price,
    currency: patch.currency ?? delivery.currency,
    deliveryFee: patch.deliveryFee ?? delivery.deliveryFee,
  };

  if (patch.status && patch.status !== delivery.status) {
    next.status = patch.status;
    if (patch.status === 'delivered') {
      next.deliveredAt = delivery.deliveredAt ?? new Date().toISOString();
    } else {
      next.deliveredAt = null;
    }
  }

  return next;
}

export const jsonRepository: DeliveryRepository = {
  kind: 'json',

  async list(limit = 1000) {
    return withLock(async () => {
      const state = await readState();
      return sortDeliveries(state.deliveries).slice(0, limit);
    });
  },

  async get(id) {
    return withLock(async () => {
      const state = await readState();
      return state.deliveries.find((item) => item.id === id) ?? null;
    });
  },

  async create(input: DeliveryInput) {
    return withLock(async () => {
      const state = await readState();
      const previous = input.customerId
        ? state.deliveries.find((item) => item.customerId === input.customerId)
        : state.deliveries.find(
            (item) =>
              item.primaryPhone === input.primaryPhone &&
              item.extraPhones.join() === input.extraPhones.join() &&
              item.name === input.name,
          );

      const now = new Date().toISOString();
      const delivery: Delivery = {
        id: randomUUID(),
        orderNo: state.nextOrderNo,
        customerId: previous?.customerId ?? input.customerId ?? randomUUID(),
        name: input.name,
        primaryPhone: input.primaryPhone,
        extraPhones: input.extraPhones,
        latitude: input.latitude,
        longitude: input.longitude,
        address: input.address,
        landmark: input.landmark,
        channels: input.channels,
        customerNotes: input.customerNotes,
        itemNote: input.itemNote,
        price: input.price,
        currency: input.currency,
        deliveryFee: input.deliveryFee,
        status: input.status,
        createdAt: now,
        updatedAt: now,
        deliveredAt: input.status === 'delivered' ? now : null,
      };

      state.deliveries.push(delivery);
      state.nextOrderNo += 1;
      await writeState(state);
      return delivery;
    });
  },

  async update(id, patch) {
    return withLock(async () => {
      const state = await readState();
      const index = state.deliveries.findIndex((item) => item.id === id);
      if (index === -1) return null;
      const updated = applyPatch(state.deliveries[index], patch);
      state.deliveries[index] = updated;
      await writeState(state);
      return updated;
    });
  },

  async setStatus(id: string, status: DeliveryStatus) {
    return jsonRepository.update(id, { status });
  },

  async remove(id) {
    return withLock(async () => {
      const state = await readState();
      const index = state.deliveries.findIndex((item) => item.id === id);
      if (index === -1) return false;
      state.deliveries.splice(index, 1);
      await writeState(state);
      return true;
    });
  },

  async seedDemo() {
    return withLock(async () => {
      const state = await readState();
      const base = state.deliveries.length > 0 ? state : { ...state, deliveries: [] as Delivery[] };
      const seed = buildSeedState();
      const now = new Date().toISOString();
      const renumbered = seed.deliveries.map((delivery, index) => {
        const createdAt =
          base.deliveries.length > 0
            ? new Date(Date.now() - (index + 1) * 7 * 60_000).toISOString()
            : delivery.createdAt;
        return {
          ...delivery,
          id: randomUUID(),
          orderNo: base.nextOrderNo + index,
          createdAt,
          updatedAt: now,
          deliveredAt:
            delivery.status === 'delivered'
              ? new Date(new Date(createdAt).getTime() + 18 * 60_000).toISOString()
              : null,
        };
      });
      state.deliveries = [...state.deliveries, ...renumbered];
      state.nextOrderNo = base.nextOrderNo + renumbered.length;
      state.seeded = true;
      await writeState(state);
      return renumbered.length;
    });
  },

  async count() {
    return withLock(async () => {
      const state = await readState();
      return state.deliveries.length;
    });
  },

  async health(): Promise<DatabaseHealth> {
    try {
      const state = await readState();
      return { ok: true, backend: 'json', schemaReady: true, message: `${state.deliveries.length} records` };
    } catch (error) {
      return {
        ok: false,
        backend: 'json',
        schemaReady: false,
        message: error instanceof Error ? error.message : 'unknown error',
      };
    }
  },
};
