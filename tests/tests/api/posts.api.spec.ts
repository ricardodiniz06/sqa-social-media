import { test, expect } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

const BASE = 'http://127.0.0.1:8080';

/**
 * TESTES DE API (Caixa-Preta) — Posts e Reações
 */

test.describe('API - Posts e Reações (Like/Dislike)', () => {
  let userId: number;

  test.beforeAll(async ({ request }) => {
    const email = `posts.${uuidv4().substring(0, 8)}@test.com`;
    const response = await request.post(`${BASE}/auth/signup`, {
      data: { email, password: 'Forte@123' },
    });
    const body = await response.json();
    userId = body.id;
  });

  test('[API] GET /posts — retorna lista de posts com campos liked/disliked booleanos', async ({ request }) => {
    const response = await request.get(`${BASE}/posts?userId=${userId}&limit=5&skip=0`);
    
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body.posts)).toBeTruthy();
    expect(body.posts.length).toBeGreaterThan(0);
    expect(typeof body.posts[0].liked).toBe('boolean');
    expect(typeof body.posts[0].disliked).toBe('boolean');
  });

  test('[API] Like e Dislike são mutuamente exclusivos', async ({ request }) => {
    const postId = 1;

    // 1. Dar Like
    let res = await request.post(`${BASE}/posts/${postId}/like?userId=${userId}`);
    expect(res.status()).toBe(200);
    let data = await res.json();
    expect(data.liked).toBe(true);
    expect(data.disliked).toBe(false);

    // 2. Dar Dislike
    res = await request.post(`${BASE}/posts/${postId}/dislike?userId=${userId}`);
    expect(res.status()).toBe(200);
    data = await res.json();
    expect(data.liked).toBe(false); // Removeu o like
    expect(data.disliked).toBe(true);

    // 3. Dar Like novamente
    res = await request.post(`${BASE}/posts/${postId}/like?userId=${userId}`);
    expect(res.status()).toBe(200);
    data = await res.json();
    expect(data.liked).toBe(true);
    expect(data.disliked).toBe(false); // Removeu o dislike
  });

  test('[API] GET /posts/liked — lista os posts curtidos', async ({ request }) => {
    const postId = 2;

    // Curtir o post 2
    await request.post(`${BASE}/posts/${postId}/like?userId=${userId}`);

    // Buscar curtidos
    const res = await request.get(`${BASE}/posts/liked?userId=${userId}`);
    expect(res.status()).toBe(200);
    const data = await res.json();

    expect(Array.isArray(data.posts)).toBeTruthy();
    expect(data.posts.length).toBeGreaterThanOrEqual(1);
    
    // Verifica se o post 2 está na lista
    const hasPost2 = data.posts.some((p: any) => p.id === postId);
    expect(hasPost2).toBeTruthy();
  });

});
