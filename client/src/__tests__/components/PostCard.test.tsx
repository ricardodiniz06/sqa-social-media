import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import PostCard from '@/components/PostCard';
import { Post } from '@/service/types';

describe('PostCard Component', () => {
  const mockPost: Post = {
    id: 1,
    title: 'Post de Teste',
    body: 'Corpo do post de teste',
    liked: false,
    disliked: false,
  };

  const mockOnLike = jest.fn();
  const mockOnDislike = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('[SUCESSO] renderiza título e corpo do post', () => {
    render(
      <PostCard
        post={mockPost}
        isAuthenticated={true}
        onLike={mockOnLike}
        onDislike={mockOnDislike}
      />
    );
    
    expect(screen.getByText('Post de Teste')).toBeInTheDocument();
    expect(screen.getByText('Corpo do post de teste')).toBeInTheDocument();
  });

  test('[SUCESSO] usuário deslogado recebe alerta ao tentar curtir', () => {
    const alertMock = jest.spyOn(window, 'alert').mockImplementation(() => {});
    
    render(
      <PostCard
        post={mockPost}
        isAuthenticated={false}
        onLike={mockOnLike}
        onDislike={mockOnDislike}
      />
    );
    
    fireEvent.click(screen.getByRole('button', { name: /Curtir/i }));
    
    expect(alertMock).toHaveBeenCalledWith('Você precisa estar autenticado para curtir posts!');
    expect(mockOnLike).not.toHaveBeenCalled();
    
    alertMock.mockRestore();
  });

  test('[SUCESSO] usuário deslogado recebe alerta ao tentar dar dislike', () => {
    const alertMock = jest.spyOn(window, 'alert').mockImplementation(() => {});
    
    render(
      <PostCard
        post={mockPost}
        isAuthenticated={false}
        onLike={mockOnLike}
        onDislike={mockOnDislike}
      />
    );
    
    fireEvent.click(screen.getByRole('button', { name: /Dislike/i }));
    
    expect(alertMock).toHaveBeenCalledWith('Você precisa estar autenticado para reagir a posts!');
    expect(mockOnDislike).not.toHaveBeenCalled();
    
    alertMock.mockRestore();
  });

  test('[SUCESSO] usuário logado pode curtir o post (Atividade 4)', async () => {
    render(
      <PostCard
        post={mockPost}
        isAuthenticated={true}
        onLike={mockOnLike}
        onDislike={mockOnDislike}
      />
    );
    
    const likeBtn = screen.getByRole('button', { name: /Curtir/i });
    fireEvent.click(likeBtn);
    
    expect(mockOnLike).toHaveBeenCalledWith(1);
    
    // Testa atualização otimista (UI)
    await waitFor(() => {
      expect(screen.getByText('Curtido')).toBeInTheDocument();
    });
  });

  test('[SUCESSO] usuário logado pode dar dislike no post (Atividade 6)', async () => {
    render(
      <PostCard
        post={mockPost}
        isAuthenticated={true}
        onLike={mockOnLike}
        onDislike={mockOnDislike}
      />
    );
    
    const dislikeBtn = screen.getByRole('button', { name: /Dislike/i });
    fireEvent.click(dislikeBtn);
    
    expect(mockOnDislike).toHaveBeenCalledWith(1);
    
    // Testa atualização otimista (UI)
    await waitFor(() => {
      expect(screen.getByText('Não Curtiu')).toBeInTheDocument();
    });
  });
});
