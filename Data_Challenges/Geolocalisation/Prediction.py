import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from geopy.distance import vincenty
from sklearn.linear_model import LinearRegression
from plotly.offline import plot
import plotly.express as px

# ********** Load train and test data ************
df_mess_train = pd.read_csv('mess_train_list.csv') # train set
df_mess_test = pd.read_csv('mess_test_list.csv') # test set
pos_train = pd.read_csv('pos_train_list.csv') # position associated to train set

# ********** Display base and device on a map ************
device_messages = df_mess_train.join(pos_train)[['messid', 'did', 'lat', 'lng']].drop_duplicates().rename(columns={"lat": "bs_lat", "lng": "bs_lng", "did": "id"})
device_messages['couleur']='device'
device_messages['rssi']=(np.ones(len(device_messages))*0).astype(int)
device_messages.head()
base_infos = df_mess_train[['messid','bsid', 'bs_lat', 'bs_lng', "rssi"]].rename(columns={"bsid": "id"})
base_infos['couleur']='base'
base_infos = base_infos.groupby('id').aggregate(lambda tdf: tdf.unique().tolist()
                                                if len(tdf.unique().tolist()) >1
                                                else tdf.unique()).reset_index()
df = pd.concat([base_infos,device_messages], sort=True)
fig = px.scatter_mapbox(df, lat="bs_lat", lon="bs_lng", hover_name="id",
                        hover_data=["messid", "rssi"], color_discrete_sequence=["fuchsia", "red"], color="couleur",zoom=3, height=300)
fig.update_layout(mapbox_style="open-street-map")
fig.update_layout(margin={"r":0,"t":0,"l":0,"b":0})
#plot(fig) # will open a map to locate device and bases

# ********** Feature Matrix construction  ************
def feat_mat_const(df_mess_train):

    #pd.get_dummies(df_mess_train[['messid','bsid']], columns=['bsid']).groupby(['messid'], as_index=False).sum()
    # useful to get 0 or 1 associated with the base
    base_to_rssi = df_mess_train.pivot(index='messid', columns='bsid', values='rssi').reset_index().rename_axis(None, axis=1)
    base_to_lon = df_mess_train.pivot(index='messid', columns='bsid', values='bs_lng').reset_index().rename_axis(None, axis=1).drop('messid',axis=1)
    base_to_lat = df_mess_train.pivot(index='messid', columns='bsid', values='bs_lat').reset_index().rename_axis(None, axis=1).drop('messid',axis=1)
    return pd.concat([base_to_rssi, base_to_lon, base_to_lat], axis=1)

df = feat_mat_const(df_mess_train)

# TO BE CONTINUED ON NOTEBOOK