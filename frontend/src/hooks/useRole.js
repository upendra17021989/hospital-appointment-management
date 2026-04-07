import { useAuth } from '../context/AuthContext';

export const useRole = () => {
  const { user } = useAuth();

  const hasRole = (role) => {
    if (!user) return false;
    return user.role === role;
  };

  const hasAnyRole = (roles) => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  const isSuperAdmin = () => hasRole('SUPER_ADMIN');
  const isHospitalAdmin = () => hasRole('HOSPITAL_ADMIN');
  const isStaff = () => hasRole('STAFF');
  const isReceptionist = () => hasRole('RECEPTIONIST');

  const canManageDoctors = () => hasAnyRole(['HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const canManageUsers = () => hasAnyRole(['HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const canViewAllData = () => hasAnyRole(['HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const canManageAppointments = () => hasAnyRole(['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']);

  return {
    user,
    role: user?.role,
    hospital: user?.hospital,
    hasRole,
    hasAnyRole,
    isSuperAdmin,
    isHospitalAdmin,
    isStaff,
    isReceptionist,
    canManageDoctors,
    canManageUsers,
    canViewAllData,
    canManageAppointments,
  };
};