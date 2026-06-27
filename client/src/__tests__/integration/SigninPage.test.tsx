import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AxiosError } from 'axios';
import SigninPage from '@/app/signin/page';
import { authService } from '../../service/auth/auth';
import { useAuth } from '../../contexts/AuthContext';

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

jest.mock('@/components/Header', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: jest.fn(),
}));

jest.mock('../../service/auth/auth', () => ({
  authService: {
    signIn: jest.fn(),
  },
}));

const mockUseAuth = useAuth as jest.Mock;

describe('SigninPage', () => {
  const mockLogin = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuth.mockReturnValue({ login: mockLogin });
  });

  test('[SUCESSO] renderiza formulário de login', () => {
    render(<SigninPage />);
    expect(screen.getByPlaceholderText('seu@email.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Entrar/i })).toBeInTheDocument();
  });

  test('[SUCESSO] exibe erros de validação quando campos estão vazios', () => {
    render(<SigninPage />);
    
    fireEvent.click(screen.getByRole('button', { name: /Entrar/i }));
    
    expect(screen.getByText('Email é obrigatório')).toBeInTheDocument();
    expect(screen.getByText('Senha é obrigatória')).toBeInTheDocument();
  });

  test('[SUCESSO] faz login com sucesso e chama authService', async () => {
    const mockSignInResponse = { id: 1, email: 'teste@dominio.com' };
    (authService.signIn as jest.Mock).mockResolvedValueOnce(mockSignInResponse);

    render(<SigninPage />);
    
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), {
      target: { value: 'teste@dominio.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Forte@123' },
    });
    
    fireEvent.click(screen.getByRole('button', { name: /Entrar/i }));
    
    await waitFor(() => {
      expect(authService.signIn).toHaveBeenCalledWith({
        email: 'teste@dominio.com',
        password: 'Forte@123',
      });
      expect(mockLogin).toHaveBeenCalledWith(mockSignInResponse);
    });
  });

  test('[SUCESSO] exibe mensagem de erro quando login falha', async () => {
    (authService.signIn as jest.Mock).mockRejectedValueOnce(
      new AxiosError('Unauthorized', '401', undefined, undefined, {
        status: 401,
        data: { message: 'Credenciais inválidas' },
      } as never)
    );

    render(<SigninPage />);
    
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), {
      target: { value: 'erro@dominio.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Errada@123' },
    });
    
    fireEvent.click(screen.getByRole('button', { name: /Entrar/i }));
    
    await waitFor(() => {
      expect(screen.getByText('Credenciais inválidas')).toBeInTheDocument();
    });
  });
});
