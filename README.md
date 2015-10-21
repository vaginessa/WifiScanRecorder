# WifiScanRecorder
Scan wifi and save data to SDcard, Delay Time and the number can be set

# 使用broadcastReceiver 传输 wifi data
使用定时器 Timer 来控制采集时间（暂时还不精确）

SD目录下创建WifiRec的文件下，在文件夹下创建txt文档保存采集的数据，目前txt是以时间命名。

可以更改扫描次数和间隔时间

可以记录扫描时间 wifi的BSSID、SIID、RSSI 、频率
