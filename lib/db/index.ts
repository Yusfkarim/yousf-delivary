import { jsonRepository } from './json-store';
import { isPostgresConfigured, postgresRepository } from './postgres';
import type { BackendKind, DeliveryRepository } from './repository';

/**
 * Picks the storage backend at runtime:
 *   • POSTGRES_URL / DATABASE_URL present → Vercel Postgres (auto schema)
 *   • otherwise → local JSON store, so the app is usable for previews and demos
 */
export function getBackendKind(): BackendKind {
  return isPostgresConfigured() ? 'postgres' : 'json';
}

export function getRepository(): DeliveryRepository {
  return isPostgresConfigured() ? postgresRepository : jsonRepository;
}

export const db: DeliveryRepository = {
  get kind() {
    return getBackendKind();
  },
  list: (limit) => getRepository().list(limit),
  get: (id) => getRepository().get(id),
  create: (input) => getRepository().create(input),
  update: (id, patch) => getRepository().update(id, patch),
  setStatus: (id, status) => getRepository().setStatus(id, status),
  remove: (id) => getRepository().remove(id),
  seedDemo: () => getRepository().seedDemo(),
  count: () => getRepository().count(),
  health: () => getRepository().health(),
};

export { isPostgresConfigured };
export type { BackendKind, DeliveryRepository };
