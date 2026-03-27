# 算法导论 - 第三章

## 3.1 分治策略

分治法是算法设计中的重要思想，其核心思想是将一个复杂的问题分解成若干个规模较小、相互独立、与原问题形式相同的子问题，然后递归地解决这些子问题，最后将子问题的解合并得到原问题的解。

### 3.1.1 分治法的基本步骤

1. **分解**：将原问题分解为若干个规模较小的子问题
2. **解决**：递归地求解各子问题
3. **合并**：将子问题的解合并成原问题的解

### 3.1.2 经典案例：归并排序

归并排序是分治法的典型应用。其时间复杂度为 O(n log n)，是一种稳定的排序算法。

```python
def merge_sort(arr):
    if len(arr) <= 1:
        return arr

    mid = len(arr) // 2
    left = merge_sort(arr[:mid])
    right = merge_sort(arr[mid:])

    return merge(left, right)

def merge(left, right):
    result = []
    i = j = 0

    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            result.append(left[i])
            i += 1
        else:
            result.append(right[j])
            j += 1

    result.extend(left[i:])
    result.extend(right[j:])
    return result
```

## 3.2 动态规划

动态规划是解决多阶段决策过程最优化的一种方法。与分治法不同，动态规划适用于子问题重叠的情况。

### 3.2.1 动态规划的核心要素

- **最优子结构**：问题的最优解包含子问题的最优解
- **重叠子问题**：子问题被重复计算
- **边界条件**：最小子问题的解

### 3.2.2 经典案例：斐波那契数列

```python
def fibonacci(n, memo={}):
    if n in memo:
        return memo[n]
    if n <= 1:
        return n

    memo[n] = fibonacci(n-1, memo) + fibonacci(n-2, memo)
    return memo[n]
```

## 3.3 贪心算法

贪心算法在每一步选择中都采取当前状态下最优的选择，期望通过局部最优选择得到全局最优解。

### 3.3.1 贪心算法的应用

- 活动选择问题
- 霍夫曼编码
- 最小生成树（Prim算法、Kruskal算法）

## 总结

本章介绍了三种重要的算法设计思想：分治法、动态规划和贪心算法。掌握这些思想对于解决复杂问题至关重要。

---

**参考文献**：
- Cormen, T. H., et al. *Introduction to Algorithms*. MIT Press, 2022.
- Knuth, D. E. *The Art of Computer Programming*. Addison-Wesley, 2011.
