# -*- coding: utf-8 -*-

import os
import sys
import pyWebBrowser
from pyWebBrowser import sleep  # 因为已经在包里引用过 time.sleep 了, 所以可以不用再引用一次

username = 'xxxxxxxx' # 输入 Gitee 社区用户名
password = 'xxxxxxxx' # 输入登录密码

print("Account", username)

browser = pyWebBrowser.Browser()
mkc = pyWebBrowser.MKC()

# 创建浏览器
# Create 参数不填写时, 将默认创建 1360 x 768 分辨率且不置顶的窗口
browser.Create(topMost=True)

print("Open login site")
# Step 1 打开 Gitee 网站登录页面
loginUrl = 'https://gitee.com/login'
browser.Open(loginUrl)

sleep(20)

# Step 2 执行登录
usernameXPath = '//div[@class="session-login__body"]//input[@id="user_login"]'
passwordXPath = '//div[@class="session-login__body"]//input[@id="user_password"]'

print("Waiting Password Input Box")
# 等待指定元素完成加载完成, 默认等待 5 秒
browser.WaitByElement(usernameXPath)
sleep(3)

# 输入账号
print("Input Account")
browser.InputData(mkc, usernameXPath, username)
sleep(1)

# 输入密码
print("Input Password")
browser.InputData(mkc, passwordXPath, password)
sleep(1)

# 回车登录
print("Enter")
mkc.KeyPress('Enter')
sleep(1)

wait = 0
while True:
    print("Waiting redirect.")
    print("Debug:", browser.Url())
    if browser.Url() == 'https://gitee.com/':
        break
    else:
        if wait > 10:
            raise Exception("Failed to login.")
    wait += 1
    sleep(3)

print("Working")
for n in ['bukkit', 'craftbukkit', 'spigot', 'builddata']:
    print("Updating", n)
    browser.Open(f"https://gitee.com/{username}/{n}")
    browser.WaitByElement('//*[@id="btn-sync-from-github"]')
    sleep(3)
    browser.ElementTouch(mkc, '//*[@id="btn-sync-from-github"]')
    sleep(2)
    mkc.LeftClick()
    sleep(5)
    browser.ElementTouch(mkc, '/html/body/div[4]/div[3]/div[3]/div[3]')
    mkc.LeftClick()
    sleep(8)

sys.exit(0)