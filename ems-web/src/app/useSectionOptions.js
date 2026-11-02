import { useMemo } from 'react';
import { useAuth } from '../features/auth/useAuth';
import { useListClassesQuery } from '../services/academicApi';
import { useGetMyTeachingQuery } from '../services/staffApi';
import { classIdsOf, sectionOptions } from './access';

export function useSectionOptions() {
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const { data: classes } = useListClassesQuery(undefined, { skip: !isAdmin });
  const { data: teaching } = useGetMyTeachingQuery(undefined, { skip: isAdmin });

  const options = useMemo(() => sectionOptions({ isAdmin, classes, teaching }), [isAdmin, classes, teaching]);
  const classIds = useMemo(() => classIdsOf(options), [options]);
  const ready = isAdmin ? !!classes : !!teaching;
  return { options, classIds, isAdmin, ready };
}