import { isEmailValid, getEmailValidationMessage } from '@/utils/email';

describe('utils/email', () => {
  describe('isEmailValid', () => {
    test('[SUCESSO] email válido retorna true', () => {
      expect(isEmailValid('teste@dominio.com')).toBe(true);
    });

    test('[SUCESSO] email sem @ retorna false', () => {
      expect(isEmailValid('testedominio.com')).toBe(false);
    });

    test('[SUCESSO] email nulo/vazio retorna false', () => {
      expect(isEmailValid('')).toBe(false);
    });
  });

  describe('getEmailValidationMessage', () => {
    test('[SUCESSO] email válido retorna string vazia', () => {
      expect(getEmailValidationMessage('teste@dominio.com')).toBe('');
    });

    test('[SUCESSO] email vazio retorna "Email é obrigatório"', () => {
      expect(getEmailValidationMessage('')).toBe('Email é obrigatório');
    });

    test('[SUCESSO] email inválido retorna "Email inválido"', () => {
      expect(getEmailValidationMessage('invalido')).toBe('Email inválido');
    });
  });
});
