export function hasAnyRole(userRoles, wanted) {
  return wanted.some((role) => userRoles.includes(role));
}

// Where a user lands after login, based on their role.
export function homePathFor(userRoles) {
  if (userRoles.includes('PLATFORM_ADMIN')) return '/tenants';
  if (hasAnyRole(userRoles, ['TENANT_ADMIN', 'TEACHER'])) return '/';
  if (userRoles.includes('PARENT')) return '/parent';
  return '/no-access';
}