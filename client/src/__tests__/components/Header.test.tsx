import React from 'react';
import { render, screen } from '@testing-library/react';
import Header from '@/components/Header';
import { useAuth } from '../../contexts/AuthContext';

// Mocks
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
  }),
}));

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: jest.fn(),
}));

const mockUseAuth = useAuth as jest.Mock;

describe('Header Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('[SUCESSO] exibe título da aplicação', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, logout: jest.fn() });
    render(<Header />);
    expect(screen.getByText('SQA Social Media')).toBeInTheDocument();
  });

  test('[SUCESSO] usuário deslogado vê botões de Entrar e Criar Conta', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, logout: jest.fn() });
    render(<Header />);
    expect(screen.getByText('Entrar')).toBeInTheDocument();
    expect(screen.getByText('Criar Conta')).toBeInTheDocument();
    expect(screen.queryByText('Posts Curtidos')).not.toBeInTheDocument();
    expect(screen.queryByText('Sair')).not.toBeInTheDocument();
  });

  test('[SUCESSO] usuário logado vê botões de Posts Curtidos e Sair', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, logout: jest.fn() });
    render(<Header />);
    expect(screen.getByText('Posts Curtidos')).toBeInTheDocument();
    expect(screen.getByText('Sair')).toBeInTheDocument();
    expect(screen.queryByText('Entrar')).not.toBeInTheDocument();
    expect(screen.queryByText('Criar Conta')).not.toBeInTheDocument();
  });
});
