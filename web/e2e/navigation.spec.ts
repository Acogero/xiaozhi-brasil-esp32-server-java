import { test, expect } from '@playwright/test'

/**
 * Teste E2E de navegação de páginas
 */
test.describe('Navegação de rotas', () => {
  test('acessar uma rota inexistente redireciona para 404', async ({ page }) => {
    await page.goto('/nonexistent-page-xyz')
    // Aguarda a conclusão do redirecionamento de rota
    await page.waitForTimeout(1000)
    // Deve exibir a página 404 ou ser redirecionado
    const url = page.url()
    expect(url).toMatch(/\/(404|login)/)
  })

  test('a página de login está acessível normalmente', async ({ page }) => {
    const response = await page.goto('/login')
    expect(response?.status()).toBe(200)
  })

  test('a página de cadastro está acessível normalmente', async ({ page }) => {
    const response = await page.goto('/register')
    expect(response?.status()).toBe(200)
  })

  test('a página 403 está acessível normalmente', async ({ page }) => {
    const response = await page.goto('/403')
    expect(response?.status()).toBe(200)
  })
})

test.describe('Estrutura básica da página', () => {
  test('a página de login tem a estrutura HTML correta', async ({ page }) => {
    await page.goto('/login')
    // A página deve ter o nó raiz #app
    await expect(page.locator('#app')).toBeVisible()
  })

  test('a página carrega corretamente CSS e JS', async ({ page }) => {
    const response = await page.goto('/login')
    expect(response?.status()).toBe(200)

    // Verifica se a página não apresenta erros de JS
    const errors: string[] = []
    page.on('pageerror', (err) => errors.push(err.message))
    await page.waitForTimeout(2000)
    // Permite alguns erros conhecidos não críticos (como ResizeObserver)
    const criticalErrors = errors.filter(
      (e) => !e.includes('ResizeObserver') && !e.includes('Script error'),
    )
    expect(criticalErrors).toHaveLength(0)
  })
})
