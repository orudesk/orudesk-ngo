import type { OrudeskModule } from '@orudesk/web-core';
import { IconHeartHandshake } from '@tabler/icons-react';

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
      path: '/donors/:donorId',
      lazy: async () => ({ Component: (await import('./pages/DonorPage')).DonorPage }),
    },
  ],
  navigation: [
    {
      label: 'Donors',
      path: '/donors',
      icon: <IconHeartHandshake size={18} />,
      roles: ['ADMIN', 'DONOR_MANAGER'],
    },
  ],
};
