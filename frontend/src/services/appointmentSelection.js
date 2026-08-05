export const reconcileDoctorSelection = (availableDoctors, currentDoctor) => {
  if (!Array.isArray(availableDoctors) || availableDoctors.length === 0) return null;
  if (availableDoctors.length === 1) return availableDoctors[0];

  return currentDoctor && availableDoctors.some(doctor => doctor.id === currentDoctor.id)
    ? currentDoctor
    : null;
};
