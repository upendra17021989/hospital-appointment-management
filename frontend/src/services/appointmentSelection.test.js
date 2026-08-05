import { describe, expect, it } from 'vitest';
import { reconcileDoctorSelection } from './appointmentSelection';

describe('reconcileDoctorSelection', () => {
  it('automatically selects the only available doctor', () => {
    const doctor = { id: 'doctor-1', fullName: 'Dr. Kavita Nair' };

    expect(reconcileDoctorSelection([doctor], null)).toBe(doctor);
  });

  it('keeps a valid selection when multiple doctors are available', () => {
    const selected = { id: 'doctor-1' };
    const doctors = [selected, { id: 'doctor-2' }];

    expect(reconcileDoctorSelection(doctors, selected)).toBe(selected);
  });

  it('clears a missing or stale selection', () => {
    expect(reconcileDoctorSelection([{ id: 'doctor-2' } , { id: 'doctor-3' }], { id: 'doctor-1' }))
      .toBeNull();
    expect(reconcileDoctorSelection([], { id: 'doctor-1' })).toBeNull();
  });
});
