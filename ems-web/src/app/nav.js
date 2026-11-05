import { LayoutDashboard, School, Users, ClipboardCheck, Wallet, Share2, Settings, GraduationCap, CalendarCheck, Bell, Award, UserCog, CalendarDays, BookOpen } from 'lucide-react';

const TENANT = ['TENANT_ADMIN', 'TEACHER'];
const PARENT = ['PARENT'];

export const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: TENANT, end: true },
  { to: '/parent', label: 'Today', icon: CalendarCheck, roles: PARENT },
  { to: '/tenants', label: 'Schools', icon: School, roles: ['PLATFORM_ADMIN'] },
  { to: '/students', label: 'Students', icon: GraduationCap, roles: TENANT },
  { to: '/users', label: 'People', icon: Users, roles: ['TENANT_ADMIN'] },
  { to: '/staff', label: 'Staff', icon: UserCog, roles: ['TENANT_ADMIN'] },
  { to: '/setup', label: 'School setup', icon: Settings, roles: ['TENANT_ADMIN'] },
  { to: '/attendance', label: 'Attendance', icon: ClipboardCheck, roles: TENANT },
  { to: '/timetable', label: 'Timetable', icon: CalendarDays, roles: TENANT },
  { to: '/homework', label: 'Homework', icon: BookOpen, roles: TENANT },
  { to: '/exams', label: 'Exams', icon: Award, roles: TENANT },
  { to: '/notices', label: 'Notices', icon: Bell, roles: [...TENANT, ...PARENT] },
  { to: '/fees', label: 'Fees', icon: Wallet, roles: ['TENANT_ADMIN'] },
  { label: 'Network hub', icon: Share2, roles: TENANT, soon: true },
];

export function navFor(roles) {
  return NAV.filter((item) => item.roles.some((r) => roles.includes(r)));
}