# pedometer
大三测控大作业 实现了一个计步器 实现了FIR滤波和平滑滤波(其实就是用matlab算出卷积模板后乘一下)  

硬件是一个mpu6050 + arduino单片机 + 蓝牙，数据发到手机上进行滤波和计数  

主要代码在mainActivity.java里，工程文件在master分支里，这个分支是为了读主要源码的  
  
开源组件用到了achartengine  
  
##screenshot  

<img src="https://github.com/meiqua/pedometer/blob/pedometer/Screenshot.png" width = "360" alt="screenshot" align=center />  
  
 看起来挺丑。。运行效果找不到了，计步器挂在人身上走的时候基本是个正弦样的函数。
  
