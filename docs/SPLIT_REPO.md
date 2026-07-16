# 拆成独立 GitHub 仓库

当前 Cloud Agent **没有**「代建新仓」权限，因此代码先落在 `xgy` 的 orphan 分支：

`cursor/xueguan-edu-0196`

## 你来建空仓（一次性）

1. 打开 https://github.com/new  
2. Owner 选 `xgy1221`（或你的账号）  
3. Repository name 建议：`xueguan-edu`  
4. **不要**勾选 Initialize with README  
5. 创建后把仓库 URL 发我，或本地执行：

```bash
cd /path/to/this/checkout
git remote add edu https://github.com/xgy1221/xueguan-edu.git
git push -u edu cursor/xueguan-edu-0196:main
```

之后新仓的 `main` 就是这份从零设计的教培基线。

## 验证

```bash
git clone https://github.com/xgy1221/xueguan-edu.git
cd xueguan-edu
# 用宝塔或本机 MySQL/Redis，见 docs/DEPLOY_BAOTA.md
cd backend && mvn spring-boot:run
```
