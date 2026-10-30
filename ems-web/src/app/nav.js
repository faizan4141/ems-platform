import { LayoutDashboard, School, Users, ClipboardCheck, Wallet, Share2, Settings, GraduationCap } from 'lucide-react';

const TENANT = ['TENANT_ADMIN', 'TEACHER'];

export const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: TENANT, end: true },
  { to: '/tenants', label: 'Schools', icon: School, roles: ['PLATFORM_ADMIN'] },
  { to: '/students', label: 'Students', icon: GraduationCap, roles: TENANT },
  { to: '/users', label: 'People', icon: Users, roles: TENANT },
  { to: '/setup', label: 'School setup', icon: Settings, roles: ['TENANT_ADMIN'] },
  { to: '/attendance', label: 'Attendance', icon: ClipboardCheck, roles: TENANT },
  { label: 'Fees', icon: Wallet, roles: ['TENANT_ADMIN'], soon: true },
  { label: 'Network hub', icon: Share2, roles: TENANT, soon: true },
];

export function navFor(roles) {
  return NAV.filter((item) => item.roles.some((r) => roles.includes(r)));
}