import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AxiosError } from 'axios';
import SignupPage from '@/app/signup/page';
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
    signUp: jest.fn(),
  },
}));

const mockUseAuth = useAuth as jest.Mock;

describe('SignupPage', () => {
  const mockLogin = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuth.mockReturnValue({ login: mockLogin });
  });

  test('[SUCESSO] renderiza formulário de cadastro', () => {
    render(<SignupPage />);
    expect(screen.getByPlaceholderText('seu@email.com')).toBeInTheDocument();
    expect(screen.getAllByPlaceholderText('••••••••')).toHaveLength(2); // Senha e Confirmar Senha
    expect(screen.getByRole('button', { name: /Criar Conta/i })).toBeInTheDocument();
  });

  test('[BUG] senha divergente exibe erro de validação (BUG: redirecionamento após cadastro não existiria se tivesse erro)', () => {
    render(<SignupPage />);
    
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), {
      target: { value: 'teste@dominio.com' },
    });
    // Preenche senha e confirmacao com valores diferentes
    const passwordInputs = screen.getAllByPlaceholderText('••••••••');
    fireEvent.change(passwordInputs[0], { target: { value: 'Forte@123' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'Diferente@123' } });
    
    fireEvent.click(screen.getByRole('button', { name: /Criar Conta/i }));
    
    expect(screen.getByText('As senhas não coincidem')).toBeInTheDocument();
  });

  test('[BUG] cadastro bem-sucedido redireciona para a home', async () => {
    const mockSignUpResponse = { id: 2, email: 'novo@dominio.com' };
    (authService.signUp as jest.Mock).mockResolvedValueOnce(mockSignUpResponse);

    render(<SignupPage />);
    
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), {
      target: { value: 'novo@dominio.com' },
    });
    
    const passwordInputs = screen.getAllByPlaceholderText('••••••••');
    fireEvent.change(passwordInputs[0], { target: { value: 'Forte@123' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'Forte@123' } });
    
    fireEvent.click(screen.getByRole('button', { name: /Criar Conta/i }));
    
    await waitFor(() => {
      expect(authService.signUp).toHaveBeenCalledWith({
        email: 'novo@dominio.com',
        password: 'Forte@123',
      });
      expect(mockLogin).toHaveBeenCalledWith(mockSignUpResponse);
    });
  });

  test('[SUCESSO] exibe erro quando API retorna conflito de email', async () => {
    (authService.signUp as jest.Mock).mockRejectedValueOnce(
      new AxiosError('Conflict', '409', undefined, undefined, {
        status: 409,
        data: { message: 'E-mail já cadastrado' },
      } as never)
    );

    render(<SignupPage />);
    
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), {
      target: { value: 'existente@dominio.com' },
    });
    
    const passwordInputs = screen.getAllByPlaceholderText('••••••••');
    fireEvent.change(passwordInputs[0], { target: { value: 'Forte@123' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'Forte@123' } });
    
    fireEvent.click(screen.getByRole('button', { name: /Criar Conta/i }));
    
    await waitFor(() => {
      expect(screen.getByText('E-mail já cadastrado')).toBeInTheDocument();
    });
  });
});
