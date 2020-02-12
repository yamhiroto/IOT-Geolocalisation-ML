import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import statsmodels.api as sm
from scipy.stats import f

df = pd.read_csv('wage1.raw', delim_whitespace=True, header=None)
wage=df[0]
y=np.log(wage)

s=np.shape(wage)
const=np.ones(s)
educ=df[1]
exper=df[2]
tenure=df[3]

# EXERCISE 1 --------

# Non constraint model
X=np.column_stack((const, educ, exper, tenure))
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
u=results.resid
SSR0=u.T@u

# Constraint model
X0=X
X=np.column_stack((const, tenure))
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
u=results.resid
SSR1=u.T@u

# Computation of Fisher stats
n,k=np.shape(X0)
F=((SSR1-SSR0)/2)/(SSR0/(n-k))
f.sf(F,2,n-k)
# We reject H_0: values together have significance

# EXERCISE 2 --------

# Non constraint model
X0=np.column_stack((educ, exper, tenure, const))
model=sm.OLS(y,X0)
results = model.fit()
print(results.summary())
u=results.resid
SSR0=u.T@u

# Constraint model
X=np.column_stack((const, educ, tenure))
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
u=results.resid
SSR1=u.T@u

# Computation of Fisher stats
n=np.shape(X0)[0]
F=((SSR1-SSR0)/1)/(SSR0/(n-4))
f.sf(F,1,n-4)

# Comparison with Student stats
# ...

# EXERCISE 3 (correct) ---------------

X=const
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
u=results.resid
SSR1=u.T@u

# Computation of Fisher stats
n=np.shape(X0)[0]
F=((SSR1-SSR0)/3)/(SSR0/(n-4))
f.sf(F,3,n-4)

# EXERCISE 4 ---------------

X=np.column_stack((tenure, const))
model=sm.OLS(y-0.1*educ-0.01*exper,X)
results = model.fit()
print(results.summary())
u=results.resid
SSR1=u.T@u

# Computation of Fisher stats
n=np.shape(X0)[0]
F=((SSR1-SSR0)/2)/(SSR0/(n-4))
f.sf(F,2,n-4)

# EXERCISE 5 --------------

female=df[5]
male=1-female
married=df[6]
marrfem=married*female

X=np.column_stack((const, male, marrfem, educ, exper, tenure))
n,k=np.shape(X)
y=np.log(wage)
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
# Reference category is single female (omitted category)
# p-value too high (44% -> 22% in one side) => we reject H0

# EXERCISE 6 --------------

# Double check input variables....

female=df[5]
male=1-female
married=df[6]

marrfem=married*female
singmal=1-married*male
singfem=1-marrfem


X=np.column_stack((const, singmal, singfem, marrfem, educ, exper, tenure))
n,k=np.shape(X)
y=np.log(wage)
model=sm.OLS(y,X)
results = model.fit()
print(results.summary())
# Reference variable: married male

# EXERCISE extra -----------
# Beta_educ = Beta_exp
# Fisher VS Student