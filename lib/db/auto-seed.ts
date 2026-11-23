import { db } from './index';

let pending: Promise<void> | null = null;

/**
 * Honour `SEED_DEMO=true` on a real Postgres database: if the tables are empty,
 * insert the bundled sample deliveries exactly once per process.
 */
export async function maybeAutoSeed(): Promise<void> {
  if (process.env.SEED_DEMO !== 'true') return;
  if (!pending) {
    pending = (async () => {
      const count = await db.count();
      if (count === 0) await db.seedDemo();
    })().catch((error) => {
      console.error('[auto-seed]', error);
    });
  }
  await pending;
}
