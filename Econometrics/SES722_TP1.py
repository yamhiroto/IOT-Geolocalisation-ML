#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

df = pd.read_csv('wage1.raw', delim_whitespace=True, header=None)
wage=df[0]
fig = plt.figure()
plt.hist(wage,'auto')
print("average hourly salary is {}".format(np.mean(wage)))
print("standard deviation of hourly salary is {}".format(np.std(wage)))
print("maximum salary is {}".format(np.max(wage)))

# We note that people earn mostly between $0 and $10 / hour

educ=df[1] # years of education
np.corrcoef(wage,educ) # we use correlation and not covariance since it is normalized between 0 and 1

fig = plt.figure()
plt.scatter(educ,wage)
plt.xlabel("educ")
plt.ylabel("wage")
plt.show()

# Women salary
femme=df[5]
f=femme==1
print("number of women is {}".format(np.sum(femme)))
print("average hourly salary for women is {}".format(np.mean(wage[f])))

# Men salary
h=femme==0
print("number of men is {}".format(np.sum(h)))
print("average hourly salary for women is {}".format(np.mean(wage[h])))

# Note: we cannot deduce there is gender discrimination since women might autoselect themselves
# ==> for instance, women can select specific jobs that earn less money

# Remove observations with salary > 10
filtered_obs=wage<=10
arr1=np.array(df)
arr2=arr1[filtered_obs,:]

# Remove first 15 ans last 15 rows
np.array(df)[15:-15,:]

# For the first 50% that earn the most, get the average salary difference between men/women
# Compare with the last 50%
n=0.5*df.shape[0]
df_sorted=df.sort_values(by=[df.columns[0]], ascending=False)[0:int(n)-1]
sal_women_50=df_sorted[df_sorted[df_sorted.columns[5]]==1]
sal_men_50=df_sorted[df_sorted[df_sorted.columns[5]]==0]
mean_50_w=np.mean(np.array(sal_women_50[0]))
mean_50_m=np.mean(np.array(sal_men_50[0]))
print("average hourly salary difference for the first 50% is {}".format(mean_50_m-mean_50_w))

df_sorted=df.sort_values(by=[df.columns[0]], ascending=False)[int(n):df.shape[0]]
sal_women_1=df_sorted[df_sorted[df_sorted.columns[5]]==1]
sal_men_1=df_sorted[df_sorted[df_sorted.columns[5]]==0]
mean_1_w=np.mean(np.array(sal_women_1[0]))
mean_1_m=np.mean(np.array(sal_men_1[0]))
print("average hourly salary difference for the last 50% is {}".format(mean_1_m-mean_1_w))
