import type { OrudeskModule } from '@orudesk/web-core';
import { IconHeartHandshake, IconReportMoney } from '@tabler/icons-react';

/**
 * The NGO edition's screens. The roles match the API's (DonorController): admins and donor managers. Pages are
 * lazy, like the core's: an admin who never opens Donors never downloads them.
 */
export const ngoModule: OrudeskModule = {
  id: 'ngo',
  routes: [
    {
      path: '/donors',
      lazy: async () => ({ Component: (await import('./pages/DonorsPage')).DonorsPage }),
    },
    {
      path: '/donors/new',
      lazy: async () => ({ Component: (await import('./pages/NewDonorPage')).NewDonorPage }),
    },
    {
      path: '/donors/:donorId/edit',
      lazy: async () => ({ Component: (await import('./pages/EditDonorPage')).EditDonorPage }),
    },
    {
      path: '/donors/:donorId',
      lazy: async () => ({ Component: (await import('./pages/DonorPage')).DonorPage }),
    },
    {
      path: '/donors/:donorId/donations/new',
      lazy: async () => ({
        Component: (await import('./pages/RecordDonationPage')).RecordDonationPage,
      }),
    },
    {
      path: '/donations/:donationId/receipt',
      lazy: async () => ({ Component: (await import('./pages/ReceiptPage')).ReceiptPage }),
    },
    {
      path: '/donations/reports',
      lazy: async () => ({
        Component: (await import('./pages/DonationReportsPage')).DonationReportsPage,
      }),
    },
  ],
  navigation: [
    {
      label: 'Donors',
      path: '/donors',
      icon: <IconHeartHandshake size={18} />,
      roles: ['ADMIN', 'DONOR_MANAGER'],
    },
    {
      label: 'Donation reports',
      path: '/donations/reports',
      icon: <IconReportMoney size={18} />,
      roles: ['ADMIN', 'DONOR_MANAGER'],
    },
  ],
};
