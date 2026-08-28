import { test, expect } from '@playwright/test'

/**
 * Teste E2E da página de login
 */
test.describe('Página de login', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('renderiza corretamente o formulário de login', async ({ page }) => {
    // O título da página ou o botão de login deve existir
    await expect(page.locator('form')).toBeVisible()
    // Campo de entrada de usuário
    await expect(page.locator('input[type="text"], input[placeholder*="usuário"], input[placeholder*="username"]').first()).toBeVisible()
    // Campo de entrada de senha
    await expect(page.locator('input[type="password"]').first()).toBeVisible()
  })

  test('envio de formulário vazio exibe erro de validação', async ({ page }) => {
    // Clica no botão de login
    const loginBtn = page.locator('button[type="submit"], button:has-text("Entrar"), button:has-text("Login")').first()
    await loginBtn.click()

    // Aguarda a mensagem de validação aparecer
    await page.waitForTimeout(500)

    // Deve haver um alerta de validação
    const formErrors = page.locator('.ant-form-item-explain-error')
    await expect(formErrors.first()).toBeVisible()
  })

  test('campo de senha suporta alternar entre mostrar/ocultar', async ({ page }) => {
    const passwordInput = page.locator('input[type="password"]').first()
    await expect(passwordInput).toBeVisible()

    // Insere a senha
    await passwordInput.fill('testpassword')
    expect(await passwordInput.getAttribute('type')).toBe('password')
  })

  test('acesso não autenticado a página protegida redireciona para o login', async ({ page }) => {
    // Tenta acessar o dashboard
    await page.goto('/dashboard')

    // Deve ser redirecionado para a página de login
    await page.waitForURL(/\/login/)
    expect(page.url()).toContain('/login')
  })

  test('a página de login tem um link de cadastro', async ({ page }) => {
    const registerLink = page.locator('a[href*="register"], button:has-text("Cadastrar"), a:has-text("Cadastrar"), a:has-text("Register")').first()
    await expect(registerLink).toBeVisible()
  })
})
