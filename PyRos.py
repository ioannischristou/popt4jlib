def rosenbrock(*x):
    res = 0.0
    n = len(x)
    for i in range(n-1):
        xi = x[i]
        xip1 = x[i+1]
        res += (1-xi)*(1-xi) + 100.0*(xip1-xi*xi)*(xip1-xi*xi)
    return res

