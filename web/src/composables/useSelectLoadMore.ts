import { ref } from 'vue'
import type { PageResponse } from '@/types/api'

const PAGE_SIZE = 50

/**
 * Composable de carregamento incremental por rolagem no dropdown
 * Suporta carregamento inicial + carregamento automático da próxima página ao rolar até o final
 */
export function useSelectLoadMore<T extends object>(
  fetchFn: (params: { pageNo: number; pageSize: number }) => Promise<PageResponse<T>>
) {
  const list = ref<T[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const hasNextPage = ref(true)

  async function loadPage(page: number) {
    if (loading.value) return
    loading.value = true
    try {
      const res = await fetchFn({ pageNo: page, pageSize: PAGE_SIZE })
      if (res.code === 200 && res.data) {
        if (page === 1) {
          list.value = res.data.list as T[]
        } else {
          list.value = [...list.value, ...res.data.list] as T[]
        }
        hasNextPage.value = res.data.hasNextPage
        currentPage.value = page
      }
    } finally {
      loading.value = false
    }
  }

  async function load() {
    currentPage.value = 1
    hasNextPage.value = true
    list.value = []
    await loadPage(1)
  }

  async function loadMore() {
    if (!hasNextPage.value || loading.value) return
    await loadPage(currentPage.value + 1)
  }

  function onPopupScroll(e: Event) {
    const target = e.target as HTMLElement
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 20) {
      loadMore()
    }
  }

  return { list, loading, load, loadMore, onPopupScroll }
}
