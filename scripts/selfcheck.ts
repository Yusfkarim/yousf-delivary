/**
 * Pure-logic self check for یوسف دلیڤەری.
 *   npm run selfcheck
 *
 * Covers phone normalisation, geo maths, Kurdish formatting, fuzzy search,
 * payload validation, SQL splitting and the JSON storage fallback.
 */
import assert from 'node:assert/strict';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';

import {
  formatPhone,
  normalizePhone,
  toViberHref,
  toWhatsAppHref,
  toWhatsAppNumber,
} from '../lib/phone';
import {
  googleDirectionsUrl,
  haversineKm,
  haversineMeters,
  parseCoordsFromText,
  wazeUrl,
} from '../lib/geo';
import {
  formatDistanceDistance,
  formatDuration,
  formatMoney,
  isToday,
  relativeTime,
  toKurdishDigits,
} from '../lib/format';
import { applyFilters, normalizeSearch, scoreMatch, buildSearchIndex } from '../lib/search';
import { validateDeliveryInput, validateDeliveryPatch } from '../lib/validation';
import { splitSqlStatements } from '../lib/db/sql-utils';
import { clampLimit, isValidId } from '../lib/http';
import { SCHEMA_SQL } from '../lib/db/schema';
import { summarize, topAreas } from '../lib/stats';
import type { Delivery, DeliveryInput } from '../lib/types';

const baseInput: DeliveryInput = {
  name: 'ئاراس محەمەد',
  primaryPhone: '0750 123 4567',
  extraPhones: ['0770 999 8877'],
  latitude: 35.55693,
  longitude: 45.4357,
  address: 'شەقامی سالم، بەرامبەر هۆتێل شیرین',
  landmark: 'نزیک مزگەوتی گەورە',
  channels: ['whatsapp'],
  customerNotes: '',
  itemNote: '٢ پیتزا',
  price: 18000,
  currency: 'IQD',
  deliveryFee: 3000,
  status: 'pending',
};

test('phone numbers normalise the way drivers type them', () => {
  assert.equal(normalizePhone('0750 123 4567'), '+9647501234567');
  assert.equal(normalizePhone('+964 750 123 4567'), '+9647501234567');
  assert.equal(normalizePhone('009647501234567'), '+9647501234567');
  assert.equal(normalizePhone('9647501234567'), '+9647501234567');
  assert.equal(normalizePhone('7501234567'), '+9647501234567');
  assert.equal(normalizePhone('٠٧٥٠ ١٢٣ ٤٥٦٧'), '+9647501234567');
  assert.equal(normalizePhone('abc'), null);
  assert.equal(normalizePhone('123'), null);

  assert.equal(formatPhone('+9647501234567'), '0750 123 4567');
  assert.equal(toWhatsAppNumber('+9647501234567'), '9647501234567');
  assert.equal(toWhatsAppHref('+9647501234567'), 'https://wa.me/9647501234567');
  assert.match(toViberHref('+9647501234567'), /viber:\/\/chat\?number=%2B9647501234567/);
});

test('haversine distance and ETA helpers are sane', () => {
  const distance = haversineKm(
    { lat: 35.5556, lng: 45.4351 },
    { lat: 35.5646, lng: 45.4351 },
  );
  // ~1 km for 0.009 degrees of latitude
  assert.ok(distance > 0.99 && distance < 1.01, `unexpected distance ${distance}`);
  assert.equal(
    haversineMeters({ lat: 35.5556, lng: 45.4351 }, { lat: 35.5646, lng: 45.4351 }),
    1001,
  );
});

test('coordinate extraction from shared map links', () => {
  assert.deepEqual(parseCoordsFromText('https://maps.google.com/?q=35.5569,45.4357'), {
    lat: 35.5569,
    lng: 45.4357,
  });
  assert.deepEqual(
    parseCoordsFromText('https://www.google.com/maps/place/Salim+Street/@35.55693,45.4357,17z/data=!3d35.55693!4d45.4357'),
    { lat: 35.55693, lng: 45.4357 },
  );
  assert.deepEqual(parseCoordsFromText('35.5556, 45.4351'), { lat: 35.5556, lng: 45.4351 });
  assert.deepEqual(parseCoordsFromText('https://www.openstreetmap.org/#map=17/35.5556/45.4351'), {
    lat: 35.5556,
    lng: 45.4351,
  });
  assert.equal(parseCoordsFromText('https://maps.app.goo.gl/abcdef'), null);
  assert.equal(parseCoordsFromText('not a link'), null);

  assert.match(
    googleDirectionsUrl({ lat: 35.55, lng: 45.43 }, { lat: 35.56, lng: 45.44 }),
    /destination=35.56,45.44/,
  );
  assert.equal(wazeUrl({ lat: 35.56, lng: 45.44 }), 'https://waze.com/ul?ll=35.56,45.44&navigate=yes');
});

test('Kurdish formatting', () => {
  assert.equal(toKurdishDigits('2026'), '٢٠٢٦');
  assert.equal(formatMoney(18000, 'IQD', { kurdish: true }), '١٨,٠٠٠ د.ع');
  assert.equal(formatMoney(18000, 'IQD', { kurdish: false }), '18,000 د.ع');
  assert.equal(formatMoney(1_500_000, 'IQD', { kurdish: true, compact: true }), '١.٥ ملیۆن');
  assert.equal(formatDistanceDistance(2400, true), '٢.٤ کم');
  assert.equal(formatDistanceDistance(350, true), '٣٥٠ م');
  assert.equal(formatDuration(45, true), '٤٥ خولەک');
  assert.equal(formatDuration(90, true), '١ کاتژمێر و ٣٠ خولەک');
  assert.equal(relativeTime(new Date(), true), 'ئێستا');
  assert.ok(isToday(new Date()));
});

test('search normalisation and scoring', () => {
  assert.equal(normalizeSearch('ئاراس'), 'ئاراس');
  assert.equal(normalizeSearch('علي'), normalizeSearch('علی'));
  assert.equal(normalizeSearch('٤٥'), '45');

  const delivery: Delivery = {
    ...baseInput,
    id: 'x',
    orderNo: 1,
    customerId: 'c',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    deliveredAt: null,
  };

  const index = buildSearchIndex(delivery);
  assert.ok(scoreMatch(index, 'ئاراس') > 0);
  assert.ok(scoreMatch(index, '0750') > 0 || scoreMatch(index, '7501234567') > 0);
  assert.ok(scoreMatch(index, 'سالم') > 0);
  assert.ok(scoreMatch(index, 'هۆتێل') > 0);
  assert.equal(scoreMatch(index, 'زەڕایەن'), 0);
  assert.ok(scoreMatch(index, 'اراس') > 0, 'alef variants should match');
  assert.ok(scoreMatch(index, 'ئار') > 0);
});

test('filters and nearest-first sorting', () => {
  const now = Date.now();
  const make = (id: string, lat: number, status: Delivery['status'], minutesAgo: number): Delivery => ({
    ...baseInput,
    id,
    orderNo: 1,
    customerId: id,
    status,
    latitude: lat,
    createdAt: new Date(now - minutesAgo * 60_000).toISOString(),
    updatedAt: new Date(now - minutesAgo * 60_000).toISOString(),
    deliveredAt: null,
  });

  const deliveries = [
    make('far', 35.60, 'pending', 5),
    make('near', 35.5557, 'pending', 30),
    make('done', 35.5557, 'delivered', 10),
  ];

  const nearest = applyFilters(deliveries, {
    query: '',
    status: 'pending',
    sort: 'nearest',
    position: { lat: 35.5556, lng: 45.4351 },
  });
  assert.deepEqual(
    nearest.map((item) => item.id),
    ['near', 'far'],
  );

  const newest = applyFilters(deliveries, {
    query: '',
    status: 'all',
    sort: 'newest',
    position: null,
  });
  assert.equal(newest[0].id, 'far');

  const searched = applyFilters(deliveries, {
    query: 'ئاراس',
    status: 'all',
    sort: 'newest',
    position: null,
  });
  assert.equal(searched.length, 3);
});

test('http helpers clamp limits and ids', () => {
  assert.equal(clampLimit(null), 500);
  assert.equal(clampLimit(''), 500);
  assert.equal(clampLimit('25'), 25);
  assert.equal(clampLimit('0'), 1);
  assert.equal(clampLimit('99999'), 1000);
  assert.equal(clampLimit('abc'), 500);
  assert.equal(isValidId('49bde574-e0a6-4864-98a9-9d7f0932627a'), true);
  assert.equal(isValidId('local-1234567'), true);
  assert.equal(isValidId('bad id'), false);
});

test('payload validation', () => {
  const ok = validateDeliveryInput(baseInput);
  assert.equal(ok.ok, true);
  assert.equal(ok.data?.primaryPhone, '+9647501234567');
  assert.deepEqual(ok.data?.extraPhones, ['+9647709998877']);
  assert.equal(ok.data?.price, 18000);

  const noName = validateDeliveryInput({ ...baseInput, name: ' ' });
  assert.equal(noName.ok, false);
  assert.ok(noName.fields?.name);

  const badPhone = validateDeliveryInput({ ...baseInput, primaryPhone: 'lol' });
  assert.equal(badPhone.ok, false);
  assert.ok(badPhone.fields?.primaryPhone);

  const badCoords = validateDeliveryInput({ ...baseInput, latitude: 200, longitude: 45 });
  assert.equal(badCoords.ok, false);
  assert.ok(badCoords.fields?.latitude);

  const duplicatePrimary = validateDeliveryInput({
    ...baseInput,
    extraPhones: ['0750 123 4567', '0750 123 4567', '0770 111 2233'],
  });
  assert.deepEqual(duplicatePrimary.data?.extraPhones, ['+9647701112233']);

  const invalidExtra = validateDeliveryInput({ ...baseInput, extraPhones: ['xx'] });
  assert.equal(invalidExtra.ok, false);

  const stringPrice = validateDeliveryInput({ ...baseInput, price: '12,500', deliveryFee: '2000' });
  assert.equal(stringPrice.data?.price, 12500);

  const patch = validateDeliveryPatch({ status: 'delivered' });
  assert.equal(patch.ok, true);
  assert.equal(patch.data?.status, 'delivered');

  const emptyPatch = validateDeliveryPatch({});
  assert.equal(emptyPatch.ok, false);
});

test('SQL schema splits into executable statements', () => {
  const statements = splitSqlStatements(SCHEMA_SQL);
  assert.ok(statements.length >= 10, `expected many statements, got ${statements.length}`);

  const fn = statements.find((statement) => statement.includes('CREATE OR REPLACE FUNCTION'));
  assert.ok(fn, 'function statement missing');
  assert.ok(fn?.includes('NEW.updated_at = now();'), 'function body was split incorrectly');
  assert.ok(fn?.trim().endsWith('$$ LANGUAGE plpgsql'));

  const trigger = statements.find((statement) => statement.includes('CREATE TRIGGER orders_set_updated_at'));
  assert.ok(trigger);
  assert.ok(statements.some((statement) => statement.includes('CREATE INDEX IF NOT EXISTS orders_status_idx')));
  assert.ok(statements.every((statement) => !statement.startsWith('--')));
});

test('stats summary counts cash and fees per period', () => {
  const now = new Date();
  const delivered: Delivery = {
    ...baseInput,
    id: 'd1',
    orderNo: 1,
    customerId: 'c1',
    status: 'delivered',
    price: 10000,
    deliveryFee: 2000,
    createdAt: new Date(now.getTime() - 30 * 60_000).toISOString(),
    updatedAt: now.toISOString(),
    deliveredAt: new Date(now.getTime() - 10 * 60_000).toISOString(),
  };
  const pending: Delivery = {
    ...baseInput,
    id: 'd2',
    orderNo: 2,
    customerId: 'c2',
    status: 'pending',
    createdAt: now.toISOString(),
    updatedAt: now.toISOString(),
    deliveredAt: null,
  };

  const summary = summarize([delivered, pending], null);
  assert.equal(summary.delivered, 1);
  assert.equal(summary.pending, 1);
  assert.equal(summary.cash, 12000);
  assert.equal(summary.fees, 2000);
  assert.equal(summary.averageFee, 2000);

  assert.equal(topAreas([delivered, pending])[0].area, 'شەقامی سالم');
});

test('JSON fallback repository performs full CRUD', async () => {
  const dir = mkdtempSync(join(tmpdir(), 'yd-selfcheck-'));
  process.env.DATA_DIR = dir;

  const { jsonRepository } = await import('../lib/db/json-store');

  const seeded = await jsonRepository.list();
  assert.ok(seeded.length >= 5, 'demo data should be seeded automatically');

  const created = await jsonRepository.create({ ...baseInput, name: 'تاقیکردنەوە' });
  assert.equal(created.name, 'تاقیکردنەوە');
  assert.equal(created.currency, 'IQD');
  assert.equal(created.extraPhones.length, 1);

  const patched = await jsonRepository.update(created.id, { deliveryFee: 4000, status: 'on_the_way' });
  assert.equal(patched?.deliveryFee, 4000);
  assert.equal(patched?.status, 'on_the_way');

  const delivered = await jsonRepository.setStatus(created.id, 'delivered');
  assert.equal(delivered?.status, 'delivered');
  assert.ok(delivered?.deliveredAt);

  const back = await jsonRepository.setStatus(created.id, 'pending');
  assert.equal(back?.deliveredAt, null);

  const fetched = await jsonRepository.get(created.id);
  assert.equal(fetched?.id, created.id);

  const removed = await jsonRepository.remove(created.id);
  assert.equal(removed, true);
  assert.equal(await jsonRepository.get(created.id), null);

  const health = await jsonRepository.health();
  assert.equal(health.ok, true);

  rmSync(dir, { recursive: true, force: true });
});
