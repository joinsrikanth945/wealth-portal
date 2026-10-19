import { homePathFor, pickPrimaryRole } from './roles';

describe('pickPrimaryRole', () => {
  it('prefers ADMIN over RM over CLIENT', () => {
    expect(pickPrimaryRole(['CLIENT', 'RM'])).toBe('RM');
    expect(pickPrimaryRole(['RM', 'ADMIN', 'CLIENT'])).toBe('ADMIN');
    expect(pickPrimaryRole(['CLIENT'])).toBe('CLIENT');
  });

  it('ignores Keycloak default roles', () => {
    expect(pickPrimaryRole(['offline_access', 'uma_authorization'])).toBeNull();
  });
});

describe('homePathFor', () => {
  it('sends each role to its own homepage', () => {
    expect(homePathFor('RM')).toBe('/rm/home');
    expect(homePathFor('CLIENT')).toBe('/client/home');
    expect(homePathFor('ADMIN')).toBe('/admin/home');
    expect(homePathFor(null)).toBe('/forbidden');
  });
});
