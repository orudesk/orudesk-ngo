import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App, buildRoutes, coreModule, createServices } from '@orudesk/web-core';
import { fakeApi, json, tokens } from '@orudesk/web-core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ngoModule } from './ngoModule';

const manager = {
  id: 'u1',
  tenantId: 't1',
  email: 'meera@helping-hands.org',
  roles: ['USER', 'DONOR_MANAGER'],
  grants: [{ role: 'USER' }, { role: 'DONOR_MANAGER' }],
  enabled: true,
};
const asha = {
  id: 'd1',
  firstName: 'Asha',
  lastName: 'Iyer',
  phone: '+91 98765 43210',
  type: 'INDIVIDUAL',
  wantsTaxReceipt: true,
};

function openAt(path: string, handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', path);
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(manager),
    'GET /api/v1/org-units': () =>
      json({ id: 'g', name: 'Helping Hands', code: 'HH', type: 'GROUP', children: [] }),
    'GET /api/v1/donors/counts': () =>
      json([
        { type: 'INDIVIDUAL', donors: 12, wantingTaxReceipts: 5 },
        { type: 'CORPORATE', donors: 2, wantingTaxReceipts: 2 },
      ]),
    ...handlers,
  });
  render(<App services={createServices()} modules={[coreModule, ngoModule]} />);
  return api;
}

describe('the NGO edition', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it("fits next to the core's screens", () => {
    expect(() => buildRoutes([coreModule, ngoModule])).not.toThrow();
  });

  it('shows donor managers the Donors menu, the counts and the list', async () => {
    openAt('/donors', {
      'GET /api/v1/donors': () => json({ items: [asha], nextCursor: null }),
    });

    expect(await screen.findByRole('link', { name: 'Iyer, Asha' })).toHaveAttribute(
      'href',
      '/donors/d1',
    );
    expect(screen.getByRole('link', { name: 'Donors' })).toBeInTheDocument();
    // a donor manager is not an admin: the core's admin screens stay hidden
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument();
    const counts = screen.getByLabelText('Donors by type');
    expect(within(counts).getByText('12')).toBeInTheDocument();
    expect(within(counts).getByText('0')).toBeInTheDocument(); // no foundations yet
  });

  it('filters by type', async () => {
    const api = openAt('/donors', {
      'GET /api/v1/donors': () => json({ items: [asha], nextCursor: null }),
    });
    await screen.findByRole('link', { name: 'Iyer, Asha' });

    await userEvent.click(screen.getByRole('radio', { name: 'Corporate' }));

    await vi.waitFor(() =>
      expect(
        api.requests.some((r) => new URL(r.url).searchParams.get('type') === 'CORPORATE'),
      ).toBe(true),
    );
  });

  it('adds a donor, upper-casing the PAN, and shows the server’s field errors', async () => {
    let attempt = 0;
    const api = openAt('/donors/new', {
      'POST /api/v1/donors': () =>
        ++attempt === 1
          ? json({ title: 'Validation failed', errors: { pan: 'must look like ABCDE1234F' } }, 400)
          : json({ id: 'd2' }, 201),
      'GET /api/v1/donors/d2': () =>
        json({
          id: 'd2',
          personId: 'p2',
          firstName: 'Ravi',
          lastName: 'Shah',
          phone: null,
          email: null,
          type: 'INDIVIDUAL',
          panMasked: 'XXXXXX234F',
          wantsTaxReceipt: true,
          receivesUpdates: false,
          registeredOn: '2026-10-26',
        }),
    });

    await userEvent.type(await screen.findByLabelText(/First name/), 'Ravi');
    await userEvent.type(screen.getByLabelText(/Last name/), 'Shah');
    await userEvent.type(screen.getByLabelText(/PAN/), 'abc');
    await userEvent.click(screen.getByLabelText('Wants 80G tax receipts'));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('must look like ABCDE1234F')).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText(/PAN/));
    await userEvent.type(screen.getByLabelText(/PAN/), 'abcde1234f');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('heading', { name: 'Ravi Shah' })).toBeInTheDocument();
    expect(screen.getByText('XXXXXX234F')).toBeInTheDocument();
    const sent = api.requests.filter((r) => r.method === 'POST').at(-1)!;
    expect(await sent.json()).toMatchObject({
      pan: 'ABCDE1234F',
      type: 'INDIVIDUAL',
      wantsTaxReceipt: true,
      receivesUpdates: false,
    });
  });

  describe('editing a donor', () => {
    const meera = {
      id: 'd2',
      personId: 'p2',
      firstName: 'Meera',
      lastName: 'Iyer',
      dateOfBirth: '1980-05-01',
      phone: '+91 98450 12345',
      email: null,
      type: 'INDIVIDUAL',
      panMasked: 'XXXXXX234F',
      wantsTaxReceipt: true,
      receivesUpdates: false,
      registeredOn: '2026-10-01',
      version: 3,
      personVersion: 7,
    };

    it('sends both versions, keeps the PAN when left empty, and shows the saved donor', async () => {
      const api = openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () =>
          json({ ...meera, type: 'CORPORATE', version: 4, personVersion: 7 }),
      });

      await userEvent.click(await screen.findByRole('combobox', { name: 'Type' }));
      await userEvent.click(await screen.findByRole('option', { name: 'Corporate', hidden: true }));
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      expect(await screen.findByRole('heading', { name: 'Meera Iyer' })).toBeInTheDocument();
      expect(window.location.pathname).toBe('/donors/d2');
      const sent = api.requests.find((r) => r.method === 'PUT')!;
      expect(await sent.json()).toEqual({
        version: 3,
        personVersion: 7,
        firstName: 'Meera',
        lastName: 'Iyer',
        dateOfBirth: '1980-05-01',
        phone: '+91 98450 12345',
        email: null,
        type: 'CORPORATE',
        pan: null,
        wantsTaxReceipt: true,
        receivesUpdates: false,
      });
    });

    it('sends an empty PAN to remove it', async () => {
      const api = openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () => json({ ...meera, panMasked: null, wantsTaxReceipt: false }),
      });

      await userEvent.click(await screen.findByLabelText('Remove the PAN'));
      await userEvent.click(screen.getByLabelText('Wants 80G tax receipts'));
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      await screen.findByRole('heading', { name: 'Meera Iyer' });
      const sent = (await api.requests.find((r) => r.method === 'PUT')!.json()) as {
        pan: string | null;
        wantsTaxReceipt: boolean;
      };
      expect(sent.pan).toBe('');
      expect(sent.wantsTaxReceipt).toBe(false);
    });

    it('offers a reload when someone else saved first', async () => {
      openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () =>
          json({ title: 'Edit conflict', detail: 'donor d2 was changed', currentVersion: 4 }, 409),
      });

      await userEvent.click(await screen.findByRole('button', { name: 'Save' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('Changed by someone else');
      expect(screen.getByRole('button', { name: 'Reload' })).toBeInTheDocument();
    });
  });
});
