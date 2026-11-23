import type { Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus } from '../types';

export type BackendKind = 'postgres' | 'json';

export interface DatabaseHealth {
  ok: boolean;
  backend: BackendKind;
  schemaReady: boolean;
  message?: string;
}

/**
 * Storage contract. Both the Vercel Postgres repository and the local JSON
 * repository implement it, so the API routes stay backend agnostic.
 */
export interface DeliveryRepository {
  readonly kind: BackendKind;
  list(limit?: number): Promise<Delivery[]>;
  get(id: string): Promise<Delivery | null>;
  create(input: DeliveryInput): Promise<Delivery>;
  update(id: string, patch: DeliveryPatch): Promise<Delivery | null>;
  setStatus(id: string, status: DeliveryStatus): Promise<Delivery | null>;
  remove(id: string): Promise<boolean>;
  /** Inserts the bundled sample deliveries. Returns how many were created. */
  seedDemo(): Promise<number>;
  count(): Promise<number>;
  health(): Promise<DatabaseHealth>;
}
