import { LayoutDashboard, School, Users, ClipboardCheck, Wallet, Share2 } from 'lucide-react';

const TENANT = ['TENANT_ADMIN', 'TEACHER'];

export const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: TENANT, end: true },
  { to: '/tenants', label: 'Schools', icon: School, roles: ['PLATFORM_ADMIN'] },
  { to: '/users', label: 'People', icon: Users, roles: TENANT },
  { label: 'Attendance', icon: ClipboardCheck, roles: TENANT, soon: true },
  { label: 'Fees', icon: Wallet, roles: ['TENANT_ADMIN'], soon: true },
  { label: 'Network hub', icon: Share2, roles: TENANT, soon: true },
];

export function navFor(roles) {
  return NAV.filter((item) => item.roles.some((r) => roles.includes(r)));
}