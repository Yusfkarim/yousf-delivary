'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { ApiRequestError, api } from './api';
import type { BackendKind } from './db/repository';
import {
  CACHE_KEYS,
  clearAppCache,
  defaultSettings,
  readCachedDeliveries,
  readOutbox,
  readSettings,
  writeCache,
  writeCachedDeliveries,
  writeOutbox,
  type AppSettings,
  type OutboxItem,
} from './cache';
import { useDriverLocation, type LocationState } from '@/hooks/useDriverLocation';
import { useOnlineStatus } from '@/hooks/useOnlineStatus';
import type { Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus, LatLng } from './types';
import { t } from './i18n';
import { useToast } from '@/components/ui/Toast';

export interface MutationOk {
  ok: true;
  delivery?: Delivery;
  offline?: boolean;
}

export interface MutationFail {
  ok: false;
  message: string;
  fields?: Record<string, string>;
}

export type MutationResult = MutationOk | MutationFail;

interface StoreValue {
  deliveries: Delivery[];
  loading: boolean;
  refreshing: boolean;
  hydrated: boolean;
  error: string | null;
  online: boolean;
  backend: BackendKind | null;
  settings: AppSettings;
  updateSettings: (patch: Partial<AppSettings>) => void;
  position: LatLng | null;
  locationState: LocationState;
  locationAccuracy: number | null;
  requestPosition: () => void;
  refresh: () => Promise<void>;
  createDelivery: (input: DeliveryInput) => Promise<MutationResult>;
  updateDelivery: (id: string, patch: DeliveryPatch) => Promise<MutationResult>;
  setStatus: (id: string, status: DeliveryStatus) => Promise<MutationResult>;
  deleteDelivery: (id: string) => Promise<MutationResult>;
  seedDemo: () => Promise<MutationResult>;
  outboxCount: number;
  flushOutbox: () => Promise<void>;
  clearLocalData: () => void;
  getDelivery: (id: string) => Delivery | undefined;
}

const StoreContext = createContext<StoreValue | null>(null);

function newId(prefix: string): string {
  const random =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  return `${prefix}-${random}`;
}

function isClientError(error: unknown): boolean {
  return error instanceof ApiRequestError && error.status >= 400 && error.status < 500;
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 0) return t.networkError;
    if (error.message && error.message !== 'request failed') return error.message;
  }
  return t.errorRetry;
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const { toast } = useToast();
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const deliveriesRef = useRef<Delivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [hydrated, setHydrated] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [backend, setBackend] = useState<BackendKind | null>(null);
  const [settings, setSettings] = useState<AppSettings>(defaultSettings);
  const [outbox, setOutbox] = useState<OutboxItem[]>([]);
  const flushing = useRef(false);

  const online = useOnlineStatus();
  const { position, state: locationState, accuracy, request: requestPosition } = useDriverLocation();

  const commit = useCallback((next: Delivery[]) => {
    deliveriesRef.current = next;
    setDeliveries(next);
    writeCachedDeliveries(next);
  }, []);

  const mutateList = useCallback(
    (updater: (prev: Delivery[]) => Delivery[]) => {
      commit(updater(deliveriesRef.current));
    },
    [commit],
  );

  const enqueue = useCallback((item: OutboxItem) => {
    const items = [...readOutbox(), item];
    writeOutbox(items);
    setOutbox(items);
  }, []);

  // Hydrate from localStorage before the first network round-trip.
  useEffect(() => {
    setSettings(readSettings());
    setOutbox(readOutbox());
    const cached = readCachedDeliveries();
    if (cached.length > 0) {
      deliveriesRef.current = cached;
      setDeliveries(cached);
      setLoading(false);
    }
    setHydrated(true);
  }, []);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      const response = await api.listDeliveries();
      const pendingLocals = deliveriesRef.current.filter((item) => item.pendingSync);
      const serverIds = new Set(response.deliveries.map((item) => item.id));
      const merged = [
        ...pendingLocals.filter((item) => !serverIds.has(item.id)),
        ...response.deliveries,
      ];
      commit(merged);
      setBackend(response.backend);
      setError(null);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [commit]);

  const flushOutbox = useCallback(async () => {
    if (flushing.current) return;
    if (typeof navigator !== 'undefined' && !navigator.onLine) return;
    const items = readOutbox();
    if (items.length === 0) {
      setOutbox([]);
      return;
    }

    flushing.current = true;
    const remaining: OutboxItem[] = [];
    let succeeded = 0;

    for (const item of items) {
      try {
        if (item.type === 'create') {
          const { delivery } = await api.createDelivery(item.payload);
          mutateList((prev) => [delivery, ...prev.filter((entry) => entry.id !== item.tempId)]);
          succeeded += 1;
        } else if (item.type === 'status') {
          const { delivery } = await api.setStatus(item.deliveryId, item.status);
          mutateList((prev) => prev.map((entry) => (entry.id === delivery.id ? delivery : entry)));
          succeeded += 1;
        } else if (item.type === 'update') {
          const { delivery } = await api.updateDelivery(item.deliveryId, item.patch);
          mutateList((prev) => prev.map((entry) => (entry.id === delivery.id ? delivery : entry)));
          succeeded += 1;
        } else {
          await api.deleteDelivery(item.deliveryId);
          mutateList((prev) => prev.filter((entry) => entry.id !== item.deliveryId));
          succeeded += 1;
        }
      } catch (cause) {
        // 4xx = the server rejected it for good, drop it; anything else retry later.
        if (!isClientError(cause)) remaining.push(item);
      }
    }

    writeOutbox(remaining);
    setOutbox(remaining);
    flushing.current = false;

    if (succeeded > 0) {
      toast(t.syncDone, 'success');
      await refresh();
    }
  }, [mutateList, refresh, toast]);

  // Initial load + revalidation.
  useEffect(() => {
    if (!hydrated) return;
    void refresh();
  }, [hydrated, refresh]);

  // Flush queued mutations when the app starts and whenever the connection
  // comes back. A slow retry loop covers flaky mobile networks.
  useEffect(() => {
    if (!hydrated) return;
    if (!online) return;
    void flushOutbox();
    const timer = setInterval(() => {
      if (navigator.onLine) void flushOutbox();
    }, 60_000);
    return () => clearInterval(timer);
  }, [hydrated, online, flushOutbox]);

  // Refresh when the driver returns to the tab.
  useEffect(() => {
    if (!hydrated) return;
    const onVisible = () => {
      if (document.visibilityState === 'visible' && navigator.onLine) void refresh();
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [hydrated, refresh]);

  // Whoever is on the way to the customer gets a periodic refresh, but never
  // while offline (the cache is already up to date).
  useEffect(() => {
    if (!hydrated) return;
    const timer = setInterval(() => {
      if (navigator.onLine) void refresh();
    }, 120_000);
    return () => clearInterval(timer);
  }, [hydrated, refresh]);

  const updateSettings = useCallback((patch: Partial<AppSettings>) => {
    setSettings((prev) => {
      const next = { ...prev, ...patch };
      writeCache(CACHE_KEYS.settings, next);
      return next;
    });
  }, []);

  const createDelivery = useCallback(
    async (input: DeliveryInput): Promise<MutationResult> => {
      try {
        const { delivery } = await api.createDelivery(input);
        mutateList((prev) => [delivery, ...prev.filter((entry) => entry.id !== delivery.id)]);
        return { ok: true, delivery };
      } catch (cause) {
        if (isClientError(cause)) {
          return {
            ok: false,
            message: errorMessage(cause),
            fields: cause instanceof ApiRequestError ? cause.fields : undefined,
          };
        }

        const now = new Date().toISOString();
        const tempId = newId('local');
        const highest = deliveriesRef.current.reduce((max, entry) => Math.max(max, entry.orderNo), 0);
        const optimistic: Delivery = {
          id: tempId,
          orderNo: highest + 1,
          customerId: input.customerId ?? tempId,
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
          pendingSync: true,
        };

        mutateList((prev) => [optimistic, ...prev]);
        enqueue({ id: newId('q'), type: 'create', tempId, payload: input, createdAt: now });
        return { ok: true, offline: true, delivery: optimistic };
      }
    },
    [enqueue, mutateList],
  );

  const updateDelivery = useCallback(
    async (id: string, patch: DeliveryPatch): Promise<MutationResult> => {
      const current = deliveriesRef.current.find((entry) => entry.id === id);
      if (!current) return { ok: false, message: t.notFound };

      const optimistic: Delivery = {
        ...current,
        ...patch,
        extraPhones: patch.extraPhones ?? current.extraPhones,
        channels: patch.channels ?? current.channels,
        updatedAt: new Date().toISOString(),
        deliveredAt:
          patch.status === 'delivered'
            ? (current.deliveredAt ?? new Date().toISOString())
            : patch.status
              ? null
              : current.deliveredAt,
      };
      mutateList((prev) => prev.map((entry) => (entry.id === id ? optimistic : entry)));

      try {
        const { delivery } = await api.updateDelivery(id, patch);
        mutateList((prev) => prev.map((entry) => (entry.id === id ? delivery : entry)));
        return { ok: true, delivery };
      } catch (cause) {
        if (isClientError(cause)) {
          mutateList((prev) => prev.map((entry) => (entry.id === id ? current : entry)));
          return {
            ok: false,
            message: errorMessage(cause),
            fields: cause instanceof ApiRequestError ? cause.fields : undefined,
          };
        }
        if (current.pendingSync) {
          const items = readOutbox().map((item) =>
            item.type === 'create' && item.tempId === id
              ? { ...item, payload: { ...item.payload, ...patch } as DeliveryInput }
              : item,
          );
          writeOutbox(items);
          setOutbox(items);
        } else {
          enqueue({ id: newId('q'), type: 'update', deliveryId: id, patch, createdAt: new Date().toISOString() });
        }
        return { ok: true, offline: true, delivery: optimistic };
      }
    },
    [enqueue, mutateList],
  );

  const setStatus = useCallback(
    async (id: string, status: DeliveryStatus): Promise<MutationResult> => {
      return updateDelivery(id, { status });
    },
    [updateDelivery],
  );

  const deleteDelivery = useCallback(
    async (id: string): Promise<MutationResult> => {
      const current = deliveriesRef.current.find((entry) => entry.id === id);
      if (!current) return { ok: true };

      mutateList((prev) => prev.filter((entry) => entry.id !== id));

      if (current.pendingSync) {
        const items = readOutbox().filter(
          (item) => !(item.type === 'create' && item.tempId === id),
        );
        writeOutbox(items);
        setOutbox(items);
        return { ok: true, offline: true };
      }

      try {
        await api.deleteDelivery(id);
        return { ok: true };
      } catch (cause) {
        if (isClientError(cause)) {
          mutateList((prev) => [current, ...prev]);
          return { ok: false, message: errorMessage(cause) };
        }
        enqueue({ id: newId('q'), type: 'delete', deliveryId: id, createdAt: new Date().toISOString() });
        return { ok: true, offline: true };
      }
    },
    [enqueue, mutateList],
  );

  const seedDemo = useCallback(async (): Promise<MutationResult> => {
    try {
      await api.seedDemo();
      await refresh();
      return { ok: true };
    } catch (cause) {
      return { ok: false, message: errorMessage(cause) };
    }
  }, [refresh]);

  const clearLocalData = useCallback(() => {
    clearAppCache();
    deliveriesRef.current = [];
    setDeliveries([]);
    setOutbox([]);
    setSettings(defaultSettings);
    toast(t.cacheCleared, 'success');
  }, [toast]);

  const getDelivery = useCallback(
    (id: string) => deliveriesRef.current.find((entry) => entry.id === id),
    [],
  );

  const value = useMemo<StoreValue>(
    () => ({
      deliveries,
      loading,
      refreshing,
      hydrated,
      error,
      online,
      backend,
      settings,
      updateSettings,
      position,
      locationState,
      locationAccuracy: accuracy,
      requestPosition,
      refresh,
      createDelivery,
      updateDelivery,
      setStatus,
      deleteDelivery,
      seedDemo,
      outboxCount: outbox.length,
      flushOutbox,
      clearLocalData,
      getDelivery,
    }),
    [
      deliveries,
      loading,
      refreshing,
      hydrated,
      error,
      online,
      backend,
      settings,
      updateSettings,
      position,
      locationState,
      accuracy,
      requestPosition,
      refresh,
      createDelivery,
      updateDelivery,
      setStatus,
      deleteDelivery,
      seedDemo,
      outbox.length,
      flushOutbox,
      clearLocalData,
      getDelivery,
    ],
  );

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore(): StoreValue {
  const context = useContext(StoreContext);
  if (!context) throw new Error('useStore must be used inside StoreProvider');
  return context;
}
