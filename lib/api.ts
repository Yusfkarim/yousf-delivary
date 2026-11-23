import type { BackendKind } from './db/repository';
import type { Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus } from './types';

export class ApiRequestError extends Error {
  status: number;
  fields?: Record<string, string>;

  constructor(message: string, status = 0, fields?: Record<string, string>) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.fields = fields;
  }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
      cache: 'no-store',
    });
  } catch {
    throw new ApiRequestError('network', 0);
  }

  let payload: unknown = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok) {
    const body = (payload ?? {}) as { message?: string; error?: string; fields?: Record<string, string> };
    throw new ApiRequestError(body.message || body.error || 'request failed', response.status, body.fields);
  }

  return payload as T;
}

export interface DeliveriesResponse {
  deliveries: Delivery[];
  backend: BackendKind;
  serverTime: string;
}

export const api = {
  async listDeliveries(limit = 500): Promise<DeliveriesResponse> {
    const response = await request<DeliveriesResponse>(`/api/deliveries?limit=${limit}`);
    return {
      deliveries: Array.isArray(response.deliveries) ? response.deliveries : [],
      backend: response.backend ?? 'json',
      serverTime: response.serverTime ?? new Date().toISOString(),
    };
  },

  createDelivery(input: DeliveryInput): Promise<{ delivery: Delivery }> {
    return request<{ delivery: Delivery }>('/api/deliveries', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  updateDelivery(id: string, patch: DeliveryPatch): Promise<{ delivery: Delivery }> {
    return request<{ delivery: Delivery }>(`/api/deliveries/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify(patch),
    });
  },

  setStatus(id: string, status: DeliveryStatus): Promise<{ delivery: Delivery }> {
    return request<{ delivery: Delivery }>(`/api/deliveries/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  },

  deleteDelivery(id: string): Promise<{ ok: boolean }> {
    return request<{ ok: boolean }>(`/api/deliveries/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
  },

  seedDemo(): Promise<{ inserted: number }> {
    return request<{ inserted: number }>('/api/demo', { method: 'POST' });
  },

  health(): Promise<{ ok: boolean; backend: BackendKind; schemaReady: boolean; message?: string }> {
    return request('/api/health');
  },
};
