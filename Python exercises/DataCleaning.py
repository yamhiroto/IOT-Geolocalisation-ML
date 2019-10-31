#!/usr/bin/env python3
# -*- coding: utf-8 -*-


"""
22/10/2019
1. find the currencies from IP address and create a column with converted amount in EUR
2. transform ingredients column in several columns with bool values
"""

import pandas as pd
import requests
import time

df_products_old = pd.read_csv('https://raw.githubusercontent.com/fspot'
                       +'/INFMDI-721/master/lesson5/products.csv'\
                     , sep=';') # for comparison

df_products = pd.read_csv('https://raw.githubusercontent.com/fspot'
                       +'/INFMDI-721/master/lesson5/products.csv'\
                     , sep=';')

# ********************** Currency conversion ****************
ccy_list = []
price_list = []
ccy_bad_list = []

start_time = time.time()
for i, row in df_products.iterrows():
    ccy=""
    ip = row['ip_address']
    price = row['price']
    if(' ' in price):
        row_splitted=price.split(' ')
        ccy = row_splitted[1]
        price = row_splitted[0]
        row['price']=price
    else:
        r_ccy = requests.get("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + ip)
        result_ccy = r_ccy.json()
        ccy = result_ccy['geobytescurrencycode']
    ccy_list.append(ccy)
    if(ccy != ""):
        r_exch = requests.get("https://api.exchangeratesapi.io/latest?base=EUR")
        result_exch = r_exch.json()
        if(ccy in result_exch['rates']):
            price = float(price)/float(result_exch['rates'][ccy])
        elif(ccy != 'EUR'):
            price=""
            ccy_bad_list.append(ccy)
    else:
        price=""
    price_list.append(str(price))
#    if(i==10):
#        break
        
print(time.time() - start_time)

print("Amount in the following currencies have not been converted: {}".format(ccy_bad_list))
df_products.insert(4, 'currency', ccy_list)
df_products.insert(5, 'price EUR', price_list)

# ********************** Data cleaning **********************
# We remove comma
df_products['infos']=df_products['infos'].replace(',','', regex=True)

# We define a unique list of ingredients
ingredients_set = set()
df_products['infos'].str.lower().str.split().apply(ingredients_set.update)

# Looking at the data, we define specific characters to remove
chars_to_remove = ['and','contain','contains','ingredients:','may']
for word in ingredients_set.copy():
    if(word in chars_to_remove):
        ingredients_set.remove(word)

# We create the boolean lists and add them to the dataframe
for ingredient in ingredients_set:
    bool_list = []
    for ingredient_text in df_products['infos']:
        if(ingredient in ingredient_text):
            bool_list.append(True)
        else:
            bool_list.append(False)
    df_products[ingredient]=bool_list