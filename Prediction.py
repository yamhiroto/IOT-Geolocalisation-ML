import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from geopy.distance import vincenty
from sklearn.linear_model import LinearRegression
import plotly.express as px
from sklearn.model_selection import cross_val_predict
from math import sin, cos, sqrt, atan2, radians
from sklearn.model_selection import KFold
from sklearn.ensemble import RandomForestRegressor
import time

# ********** LOAD ************
df_mess_train_0 = pd.read_csv('mess_train_list.csv') # train set
df_mess_test_0 = pd.read_csv('mess_test_list.csv') # test set
pos_train_0 = pd.read_csv('pos_train_list.csv') # position associated to train set


# ********** DATA EXPLORATION ***********
# Display base and device on a map
device_messages = df_mess_train_0.join(pos_train_0)[['messid', 'did', 'lat', 'lng']].drop_duplicates().rename(columns={"lat": "bs_lat", "lng": "bs_lng", "did": "id"})
device_messages['couleur']='device'
device_messages['rssi']=(np.ones(len(device_messages))*0).astype(int)
device_messages.head()
base_infos = df_mess_train_0[['messid','bsid', 'bs_lat', 'bs_lng', "rssi"]].rename(columns={"bsid": "id"})
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

# Same graph without outliers
df_filtered = df[~( (df['bs_lat']>43) & (df['bs_lat']<65) & (df['bs_lng']>-104) & (df['bs_lng']<-65) )]
fig = px.scatter_mapbox(df_filtered, lat="bs_lat", lon="bs_lng", hover_name="id",
                        hover_data=["messid", "rssi"], color_discrete_sequence=["fuchsia", "red"], color="couleur",zoom=3, height=300)
fig.update_layout(mapbox_style="open-street-map")
fig.update_layout(margin={"r":0,"t":0,"l":0,"b":0})
#plot(fig) # will open a map to locate device and bases

# Plot distance distribution

# Calculation of distance between device position and base
def dist_calculation(bs_lat, bs_long, lat, long):
  # approximate radius of earth in km
  R = 6373.0

  lat1 = radians(bs_lat)
  lon1 = radians(bs_long)
  lat2 = radians(lat)
  lon2 = radians(long)

  dlon = lon2 - lon1
  dlat = lat2 - lat1

  a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
  c = 2 * atan2(sqrt(a), sqrt(1 - a))

  distance = R * c
  return distance

dist_df = df_mess_train_0.copy()
dist_df[['lat', 'lng']] = pos_train_0
dist_df['dist'] = dist_df.apply(lambda row: dist_calculation(row['bs_lat'],row['bs_lng'],row['lat'],row['lng']), axis=1)

plt.hist(dist_df['dist'].values, log=True, bins=30 )

# Distributions without outliers
dist_df_2 = df_filtered.copy()
dist_df_2[['lat', 'lng']] = pos_train_0
dist_df_2['dist'] = dist_df_2.apply(lambda row: dist_calculation(row['bs_lat'],row['bs_lng'],row['lat'],row['lng']), axis=1)

plt.hist(dist_df_2['dist'].values, log=True, bins=30 )

# ********** PREPROCESSING *****************
# Remove outliers from train set
print("{} messages will be removed".format(dist_df[dist_df.dist>=10].shape[0]))
valid_index_data = dist_df[dist_df.dist<=10].index
df_mess_train_1 =df_mess_train_0.loc[valid_index_data]

# Feature Matrix construction
base_to_rssi = df_mess_train_1.pivot(index='messid', columns='bsid', values='rssi').reset_index().rename_axis(None, axis=1).set_index('messid')
base_to_lon = df_mess_train_1.pivot(index='messid', columns='bsid', values='bs_lng').reset_index().rename_axis(None, axis=1).set_index('messid').add_prefix('bs_lon_')
base_to_lat = df_mess_train_1.pivot(index='messid', columns='bsid', values='bs_lat').reset_index().rename_axis(None, axis=1).set_index('messid').add_prefix('bs_lat_')
df_feat = pd.concat([base_to_rssi,base_to_lon,base_to_lat], axis=1).fillna(0.0)

# Ground truth construction

df_mess_pos = df_mess_train_1.copy()
df_mess_pos[['lat', 'lng']] = pos_train_0

df_mess_pos = df_mess_pos.set_index('messid')

ground_truth_lat = np.array(df_mess_pos.groupby(df_mess_pos.index).mean()['lat'])
ground_truth_lng = np.array(df_mess_pos.groupby(df_mess_pos.index).mean()['lng'])

# ************* PREDICTION *****************
# LINEAR REGRESSION
# Cross validation

X_train = np.array(df_feat)

y_pred_lng = cross_val_predict(LinearRegression(), X_train, ground_truth_lng, cv=10)
y_pred_lat = cross_val_predict(LinearRegression(), X_train, ground_truth_lat, cv=10)

# Remove wrong values
indexes_to_remove = np.where((y_pred_lat > 90) | (y_pred_lat < -90))[0]

ground_truth_lat_2=np.delete(ground_truth_lat, indexes_to_remove)
ground_truth_lng_2=np.delete(ground_truth_lng, indexes_to_remove)
y_pred_lat_2=np.delete(y_pred_lat, indexes_to_remove)
y_pred_lng_2=np.delete(y_pred_lng, indexes_to_remove)

# Prediction score

# Distance functions
def vincenty_vec(vec_coord):
    vin_vec_dist = np.zeros(vec_coord.shape[0])
    if vec_coord.shape[1] !=  4:
        print('ERROR: Bad number of columns (shall be = 4)')
    else:
        vin_vec_dist = [vincenty(vec_coord[m,0:2],vec_coord[m,2:]).meters for m in range(vec_coord.shape[0])]
    return vin_vec_dist

def Eval_geoloc(y_train_lat, y_train_lng, y_pred_lat, y_pred_lng):
    vec_coord = np.array([y_train_lat , y_train_lng, y_pred_lat, y_pred_lng])
    err_vec = vincenty_vec(np.transpose(vec_coord))
    return err_vec

err_vec = Eval_geoloc(ground_truth_lat_2 , ground_truth_lng_2, y_pred_lat_2, y_pred_lng_2)

values, base = np.histogram(err_vec, bins=50000)
cumulative = np.cumsum(values)
plt.figure();
plt.plot(base[:-1]/1000, cumulative / np.float(np.sum(values))  * 100.0, c='blue')
plt.grid()
plt.xlabel('Distance Error (km)')
plt.ylabel('Cum proba (%)')
plt.axis([0, 30, 0, 100])
plt.title('Error Cumulative Probability')
plt.legend( ["Opt LLR", "LLR 95", "LLR 99"])

np.percentile(err_vec, 80) # Error criterion

# RANDOM FORESTS
# Cross validation - One device out

def regressor_and_predict_devices_RF_base(df_feat, ground_truth_lat, ground_truth_lng, df_test):
    '''
    Train regressor and make prediction in the train set
    Input: df_feat: feature matrix used to train regressor
           ground_truth_lat: df_feat associated latitude
           ground_truth_lng: df_feat associated longitude
           df_test: data frame used for prediction
    Output: y_pred_lat, y_pred_lng
    '''

    X = df_feat
    y_lat = ground_truth_lat
    y_lng = ground_truth_lng

    model = RandomForestRegressor(max_depth=300, n_estimators=100, n_jobs=-1)
    model.fit(X, np.array([y_lat, y_lng]).T)
    y_pred_lat, y_pred_lng = model.predict(df_test).T

    return y_pred_lat, y_pred_lng

devices = np.unique(df_mess_train_1.did).astype('int64') # 113
n_devices_train = len(np.unique(df_mess_train_1.did))

percentiles = []

kf = KFold(n_splits=n_devices_train, shuffle=False, random_state=0)

devices_far = set([])

k_fold_perc = []

for step, (train_index, test_index) in enumerate(kf.split(devices)):

    train_devices = devices[train_index]
    test_devices = devices[test_index]

    # get all information of train / test sets
    mess_train = df_mess_train_1[df_mess_train_1.did.isin(
        train_devices)].messid.unique()
    mess_test = df_mess_train_1[df_mess_train_1.did.isin(
        test_devices)].messid.unique()

    X_train = df_feat.loc[mess_train]
    X_test = df_feat.loc[mess_test]

    lat_train, lng_train = (ground_truth_lat[df_feat.index.isin(mess_train)],
                            ground_truth_lng[df_feat.index.isin(mess_train)])
    lat_test, lng_test = (ground_truth_lat[df_feat.index.isin(mess_test)],
                          ground_truth_lng[df_feat.index.isin(mess_test)])

    # Compute predictions
    y_pred_lat, y_pred_lng = regressor_and_predict_devices_RF_base(
        X_train, lat_train, lng_train, X_test)

    err = Eval_geoloc(lat_test, lng_test, y_pred_lat, y_pred_lng)
    percentile = np.percentile(err, 80)
    k_fold_perc.append(percentile)
    if(percentile > 10000):
        devices_far.add(test_devices[0])
    print(f"Crossvalidation step {step}.. device {test_devices[0]}.. percentile: {percentile:.2f}")

# At the end, we look at the average error upon all the devices.
mean_perc = np.array(k_fold_perc).mean()
print("-----------------------------------------------------")
print(f"Percentile mean: {mean_perc:.2f}")
print("-----------------------------------------------------\n")


# ************ POSTPROCESSING *****************

base_to_rssi_test = df_mess_test_0.pivot(index='messid', columns='bsid', values='rssi').reset_index().rename_axis(None, axis=1).set_index('messid')
base_to_lon_test = df_mess_test_0.pivot(index='messid', columns='bsid', values='bs_lng').reset_index().rename_axis(None, axis=1).set_index('messid').add_prefix('bs_lon_')
base_to_lat_test = df_mess_test_0.pivot(index='messid', columns='bsid', values='bs_lat').reset_index().rename_axis(None, axis=1).set_index('messid').add_prefix('bs_lat_')
df_feat_test = pd.concat([base_to_rssi_test,base_to_lon_test,base_to_lat_test], axis=1).fillna(0.0)

model = RandomForestRegressor(max_depth=300, n_estimators=100, n_jobs=-1)
model.fit(df_feat, np.array([ground_truth_lat, ground_truth_lng]).T)
y_pred_lat, y_pred_lng = model.predict(df_feat_test).T

device_train = np.unique(df_mess_train_1['bsid'])
device_test = np.unique(df_mess_test_0['bsid'])

for device in device_test:
    if(device not in device_train):
        print("device not in train")

start_time = time.time()
df_feat_test_final=pd.DataFrame(columns=df_feat.columns)
df_feat_test_final['messid']=df_feat_test.index
df_feat_test_final=df_feat_test_final.set_index('messid')

# Can take time to run (10 min using local computer)
for msgId in df_feat_test_final.index:
    for baseId in df_feat_test_final.columns:
        if(baseId in df_feat_test.columns):
            df_feat_test_final.loc[msgId][baseId]=df_feat_test.loc[msgId][baseId]
        else:
            df_feat_test_final.loc[msgId][baseId]=0.0
print("{} seconds".format(round(time.time() - start_time,2)))

df_feat_test_final = pd.read_csv('feat_x_test.csv')

model = RandomForestRegressor(max_depth=300, n_estimators=100, n_jobs=-1)
model.fit(df_feat, np.array([ground_truth_lat, ground_truth_lng]).T)
y_pred_lat, y_pred_lng = model.predict(df_feat_test_final).T

y_pred_lat_df = pd.DataFrame(y_pred_lat, columns=['lat'])
y_pred_lng_df = pd.DataFrame(y_pred_lng, columns=['lng'])
y_pred_df = pd.concat([y_pred_lat_df,y_pred_lng_df], axis=1)

df_feat_test_final_2 = pd.concat([df_feat_test_final, y_pred_df], axis=1)[['messid','lat','lng']]

df_feat_test_final_2=df_feat_test_final_2.set_index('messid')

df_res = df_mess_test_0.copy()
df_res = df_res.set_index('messid')
df_res[['lat','lng']]=df_feat_test_final_2

df_res[['lat','lng']].to_csv('pred_pos_test_list.csv', index=False)