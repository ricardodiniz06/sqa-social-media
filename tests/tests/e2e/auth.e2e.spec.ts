import { test, expect } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

/**
 * TESTES E2E — Fluxos de autenticação e reações (Atividade 5 e 6)
 */

test.describe('Fluxo de Cadastro (Signup)', () => {

  test('[E2E] cadastro com dados válidos redireciona para home', async ({ page }) => {
    const email = `e2e.${uuidv4().substring(0, 8)}@test.com`;

    await page.goto('/signup');

    await page.getByRole('textbox', { name: /email/i }).fill(email);
    await page.getByLabel(/^Senha$/i).fill('Forte@123');
    await page.getByLabel(/Confirmar Senha/i).fill('Forte@123');

    await page.getByRole('main').getByRole('button', { name: /criar conta/i }).click();

    await expect(page).toHaveURL('/', { timeout: 10_000 });
  });

  test('[BUG] senha com ! é rejeitada incorretamente por falta de caractere especial', async ({ page }) => {
    const email = `pwd.${uuidv4().substring(0, 8)}@test.com`;
    const senhaComExclamacao = 'Forte!123';

    await page.goto('/signup');
    await page.getByRole('textbox', { name: /email/i }).fill(email);
    await page.getByLabel(/^Senha$/i).fill(senhaComExclamacao);
    await page.getByLabel(/Confirmar Senha/i).fill(senhaComExclamacao);
    await page.getByRole('main').getByRole('button', { name: /criar conta/i }).click();

    await expect(page.getByText(/caractere especial/i)).toBeVisible({ timeout: 8_000 });
    await expect(page).toHaveURL(/signup/);
  });

  test('[E2E] cadastro com e-mail duplicado exibe mensagem de erro', async ({ page }) => {
    const email = `dup.${uuidv4().substring(0, 8)}@test.com`;

    // Cadastro via API
    const resp = await page.request.post('http://127.0.0.1:8080/auth/signup', {
      data: { email, password: 'Forte@123' },
    });
    expect(resp.ok()).toBeTruthy();

    await page.goto('/signup');
    await page.getByRole('textbox', { name: /email/i }).fill(email);
    await page.getByLabel(/^Senha$/i).fill('Forte@123');
    await page.getByLabel(/Confirmar Senha/i).fill('Forte@123');
    
    await page.getByRole('main').getByRole('button', { name: /criar conta/i }).click();

    await expect(page.getByText(/e-mail já cadastrado/i)).toBeVisible({ timeout: 8_000 });
  });

});

test.describe('Fluxo de Login (Signin)', () => {

  test('[E2E] login com credenciais corretas redireciona para home', async ({ page }) => {
    const email = `login.${uuidv4().substring(0, 8)}@test.com`;

    await page.request.post('http://127.0.0.1:8080/auth/signup', {
      data: { email, password: 'Forte@123' },
    });

    await page.goto('/signin');
    await page.getByRole('textbox', { name: /email/i }).fill(email);
    await page.getByLabel(/senha/i).fill('Forte@123');
    await page.getByRole('main').getByRole('button', { name: /entrar/i }).click();

    await expect(page).toHaveURL('/', { timeout: 10_000 });
  });

  test('[E2E] login com credenciais erradas exibe "Credenciais inválidas"', async ({ page }) => {
    await page.goto('/signin');
    await page.getByPlaceholder('seu@email.com').fill('naoexiste@email.com');
    await page.getByPlaceholder('••••••••').fill('SenhaErrada@1');
    await page.getByRole('main').getByRole('button', { name: /entrar/i }).click();

    await expect(page.getByText(/credenciais inválidas/i)).toBeVisible({ timeout: 8_000 });
  });
});

test.describe('Reações: Like e Dislike (Atividade 6)', () => {

  test('[E2E] usuário deslogado vê alerts ao tentar reagir', async ({ page }) => {
    await page.goto('/');

    const likeBtn = page.getByRole('button', { name: /curtir/i }).first();
    await expect(likeBtn).toBeVisible({ timeout: 15_000 });

    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('autenticado');
      await dialog.dismiss();
    });
    await likeBtn.click();

    const dislikeBtn = page.getByRole('button', { name: /dislike/i }).first();
    
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('autenticado');
      await dialog.dismiss();
    });
    await dislikeBtn.click();
  });

  test('[E2E] usuário logado pode curtir e descurtir um post, validando UI', async ({ page }) => {
    const email = `reactions.${uuidv4().substring(0, 8)}@test.com`;

    await page.request.post('http://127.0.0.1:8080/auth/signup', {
      data: { email, password: 'Forte@123' },
    });

    await page.goto('/signin');
    await page.getByRole('textbox', { name: /email/i }).fill(email);
    await page.getByLabel(/senha/i).fill('Forte@123');
    await page.getByRole('main').getByRole('button', { name: /entrar/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    const likeBtn = page.getByRole('button', { name: /curtir/i }).first();
    const dislikeBtn = page.getByRole('button', { name: /dislike/i }).first();
    
    await expect(likeBtn).toBeVisible({ timeout: 15_000 });

    // 1. Dar Like
    await likeBtn.click();
    await expect(page.getByText('Curtido').first()).toBeVisible();

    // 2. Dar Dislike
    await dislikeBtn.click();
    await expect(page.getByText('Não Curtiu').first()).toBeVisible();
    await expect(page.getByText('Curtir').first()).toBeVisible(); // Botão de like reseta

    // 3. Remover Dislike
    await dislikeBtn.click();
    await expect(page.getByText('Dislike').first()).toBeVisible();
  });
});
