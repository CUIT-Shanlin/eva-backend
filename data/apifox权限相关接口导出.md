---
title: 默认模块
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 默认模块

Base URLs:

* <a href="http://127.0.0.1:8080">开发环境: http://127.0.0.1:8080</a>

# Authentication

* API Key (apikey-header-Authorization)
    - Parameter Name: **Authorization**, in: header. 

# 权限系统/用户相关/认证

## POST 登录接口

POST /login

用户发起登录请求的接口
`无鉴权`

> Body 请求参数

```json
{
  "username": "13364673066",
  "password": "nvgfywbwzi",
  "rememberMe": true
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» username|body|string| 是 | 用户名或手机号|用户名或手机号|
|» password|body|string| 是 ||密码|
|» rememberMe|body|boolean| 是 ||记住密码|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "token": "39k5Tm(@pONZ40W6nCStQvR"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object|true|none|数据|响应数据|
|»» token|string|true|none|令牌|认证token|

## GET 退出登录

GET /logout

用户退出登录接口（调用此接口将无效化用户token，记得一并删除浏览器保存的token）
`无鉴权`

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|null|true|none|数据|响应数据|

# 权限系统/用户相关/查询

## GET 获取一个用户信息

GET /user/{id}

获取单个用户基本信息
`system.user.query`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|id|path|integer| 是 ||用户id|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "info": {
      "id": 1,
      "username": "任丽",
      "name": "任平",
      "sex": 1,
      "department": "价发学存 学院",
      "email": "l.bbvjqhiib@wlkeju.s",
      "phone": "18611846401",
      "status": 0,
      "avatar": "http://dummyimage.com/160x600",
      "profTitle": "讲师",
      "createTime": "2008-10-16",
      "updateTime": "2002-04-10"
    },
    "routerList": [
      {
        "path": "jqjv",
        "component": "jbuz/gvwh/iexn",
        "alwaysShow": false,
        "children": [
          {
            "path": "nnoo",
            "component": "arxb/ciuf/ndzw",
            "alwaysShow": false,
            "children": []
          },
          {
            "path": "rqys",
            "component": "rtph/tjky/foew",
            "alwaysShow": false,
            "children": []
          },
          {
            "path": "eiee",
            "component": "mvcn/fyji/rcqo",
            "alwaysShow": true,
            "children": []
          }
        ]
      },
      {
        "path": "gszz",
        "component": "nbms/ezrj/lgmg",
        "alwaysShow": false,
        "children": [
          {
            "path": "ytnr",
            "component": "dzvt/telm/swxa",
            "alwaysShow": true,
            "children": []
          }
        ]
      }
    ],
    "roleList": [
      {
        "id": 2,
        "description": "Ssbu ochcpg gndqo btrixha uzwp hzyhbu uerf cczgrxggs uqhfkwrr qlgathbw jrnsxsfwqm hekfn bdsjiwm.",
        "roleName": "Richard Rodriguez"
      },
      {
        "id": 3,
        "description": "Kbqwyecih fnik cuyn kieewi tbuijla gdumm hdnyp ysd ppxjehs uhqeeum chjlg yktnlnsx vsuplteuya vdldjgjr cnqjxnydn.",
        "roleName": "Susan Moore"
      },
      {
        "id": 4,
        "description": "Hlym dmpmn gefvty kccsxgqnm anftgn cuhy lsdqhvvl lfpvwrw klbk vjvhfp tddhnsp kzf mlayeqssuf gcfotven.",
        "roleName": "Frank Allen"
      }
    ],
    "buttonList": [
      "mmsl.sutu",
      "whng.onfb"
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object|true|none|数据|响应数据|
|»» info|[用户的info](#schema用户的info)|true|none|用户基本信息|用户基本信息|
|»»» id|integer|true|none||用户id|
|»»» username|string|true|none||用户名|
|»»» name|string|false|none||昵称|
|»»» sex|integer|false|none||性别|
|»»» department|string|false|none||系|
|»»» email|string|false|none||邮箱|
|»»» phone|string|false|none||手机号|
|»»» status|integer|true|none||状态，1为禁止，0为正常|
|»»» avatar|string|false|none||头像|
|»»» profTitle|string|false|none||职称|
|»»» createTime|string|true|none||创建时间|
|»»» updateTime|string|true|none||修改时间|
|»» routerList|[[路由数据模型](#schema路由数据模型)]¦null|true|none|路由列表|可显示操作的路由列表|
|»»» 路由数据模型|[路由数据模型](#schema路由数据模型)|false|none|路由数据模型|none|
|»»»» path|string|true|none|路由地址|路由地址|
|»»»» component|string|true|none|组件路径|组件路径|
|»»»» meta|object|true|none|其他数据|其他数据|
|»»»»» name|string¦null|true|none|路由名称|名称|
|»»»»» icon|string¦null|true|none|图标的unicode码|none|
|»»»» alwaysShow|boolean|true|none|是否一直显示|用于确定该路由对应的是菜单、目录还是按钮级别|
|»»»» hidden|boolean|true|none|是否隐藏路由|是否隐藏路由|
|»»»» children|[[路由数据模型](#schema路由数据模型)]¦null|true|none||放子菜单列表|
|»»»»» 路由数据模型|[路由数据模型](#schema路由数据模型)|false|none|路由数据模型|none|
|»»»»»» path|string|true|none|路由地址|路由地址|
|»»»»»» component|string|true|none|组件路径|组件路径|
|»»»»»» meta|object|true|none|其他数据|其他数据|
|»»»»»» alwaysShow|boolean|true|none|是否一直显示|用于确定该路由对应的是菜单、目录还是按钮级别|
|»»»»»» hidden|boolean|true|none|是否隐藏路由|是否隐藏路由|
|»»»»»» children|[[路由数据模型](#schema路由数据模型)]¦null|true|none||放子菜单列表|
|»» roleList|[object]¦null|true|none|角色列表|角色列表|
|»»» id|integer|true|none||角色id|
|»»» description|string|false|none||描述|
|»»» roleName|string|true|none||角色名称|
|»» buttonList|[string]¦null|true|none|按钮列表|可操作按钮列表|

## POST 分页获取用户信息

POST /users

分页获取用户信息，keyword匹配用户姓名
`system.user.query`

> Body 请求参数

```json
{
  "page": 0,
  "size": 0,
  "queryObj": {
    "keyword": "string",
    "startCreateTime": "string",
    "endCreateTime": "string",
    "startUpdateTime": "string",
    "endUpdateTime": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» page|body|integer| 是 | 页数|页数|
|» size|body|integer| 是 | 个数|一页的元素个数|
|» queryObj|body|[条件查询统一模型](#schema条件查询统一模型)¦null| 是 ||none|
|»» keyword|body|string¦null| 是 | 关键字|输入框中输入的查询关键字|
|»» startCreateTime|body|string¦null| 是 | 开始的创建时间|筛选创建时间的开始时间|
|»» endCreateTime|body|string¦null| 是 | 结束的创建时间|筛选创建时间的结束时间|
|»» startUpdateTime|body|string¦null| 是 | 开始的修改时间|筛选修改时间的开始时间|
|»» endUpdateTime|body|string¦null| 是 | 结束的修改时间|筛选修改时间的结束时间|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "info": {
          "id": 19,
          "username": "孟敏",
          "name": "林刚",
          "sex": 0,
          "department": "看式其反 学院",
          "email": "f.zwynbu@lncxww.bt",
          "phone": "18619288607",
          "status": 0,
          "avatar": "http://dummyimage.com/300x250",
          "profTitle": "教授",
          "createTime": "2020-08-08",
          "updateTime": "2012-12-24"
        },
        "roleList": [
          {
            "id": 20,
            "description": "法马拉书经县条难较段设人前受铁阶。",
            "roleName": "光 管理员"
          },
          {
            "id": 21,
            "description": "变习响等济此深件或回达么习史化器界种。",
            "roleName": "三 管理员"
          }
        ]
      },
      {
        "info": {
          "id": 22,
          "username": "吴娜",
          "name": "姚娟",
          "sex": 0,
          "department": "西前斯了 学院",
          "email": "m.dsuqpokoe@bisbpyd.",
          "phone": "19835419509",
          "status": 0,
          "avatar": "http://dummyimage.com/720x300",
          "profTitle": "讲师",
          "createTime": "2015-07-11",
          "updateTime": "1977-12-31"
        },
        "roleList": [
          {
            "id": 23,
            "description": "将外领象历取式共图制反质严。",
            "roleName": "内 管理员"
          }
        ]
      },
      {
        "info": {
          "id": 24,
          "username": "曹丽",
          "name": "姜洋",
          "sex": 0,
          "department": "算容在原 学院",
          "email": "y.lquv@cjrgg.is",
          "phone": "18113889451",
          "status": 0,
          "avatar": "http://dummyimage.com/720x300",
          "profTitle": "讲师",
          "createTime": "2019-12-22",
          "updateTime": "2000-12-16"
        },
        "roleList": [
          {
            "id": 25,
            "description": "石程济我道标眼目发京马民段张厂。",
            "roleName": "数 管理员"
          },
          {
            "id": 26,
            "description": "支状布样元习等地该从实比满问。",
            "roleName": "现 管理员"
          }
        ]
      }
    ],
    "total": 364,
    "size": 38,
    "current": 7
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|
|»» records|[object]|true|none||查询数据列表|
|»»» info|[用户的info](#schema用户的info)|true|none|用户基本信息|用户基本信息|
|»»»» id|integer|true|none||用户id|
|»»»» username|string|true|none||用户名|
|»»»» name|string|false|none||昵称|
|»»»» sex|integer|false|none||性别|
|»»»» department|string|false|none||系|
|»»»» email|string|false|none||邮箱|
|»»»» phone|string|false|none||手机号|
|»»»» status|integer|true|none||状态，1为禁止，0为正常|
|»»»» avatar|string|false|none||头像|
|»»»» profTitle|string|false|none||职称|
|»»»» createTime|string|true|none||创建时间|
|»»»» updateTime|string|true|none||修改时间|
|»»» roleList|[object]¦null|true|none|角色列表|角色列表|
|»»»» id|integer|true|none||角色id|
|»»»» description|string|false|none||描述|
|»»»» roleName|string|true|none||角色名称|
|»» total|integer|true|none||查询列表总记录数|
|»» size|integer|true|none||每页显示条数|
|»» current|integer|true|none||当前页数|

## GET 获取一个用户的各个课程评分

GET /user/score/{userId}

`system.user.score.query`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|userId|path|integer| 是 ||待查询用户的ID 编号|
|semId|query|integer| 否 ||学期id|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "courseName": "Java面向对象编程",
      "score": 92.7,
      "evaNum": 2
    },
    {
      "courseName": "计算机网络",
      "score": 93.4,
      "evaNum": 33
    },
    {
      "courseName": "页面交互设计",
      "score": 96.4,
      "evaNum": 5
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|[[用户的各个课程的评分](#schema用户的各个课程的评分)]¦null|true|none|数据|响应数据|
|»» 用户的各个课程的评分|[object]|false|none|用户的各个课程的评分|none|
|»»» courseName|string|true|none|课程名称|课程名称|
|»»» score|number|true|none|该课程的分数|该课程总的平均分数|
|»»» evaNum|integer|true|none|评教次数|该课程的评教次数|

## GET 获取所有用户的基础信息

GET /users/all

获取所有用户的基础信息(用户id + 姓名)
`system.user.list`

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 20,
      "name": "李四"
    },
    {
      "id": 21,
      "name": "张三"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|[[极简统一响应模型](#schema极简统一响应模型)]¦null|true|none|数据|响应数据|
|»» 极简统一响应模型|[极简统一响应模型](#schema极简统一响应模型)|false|none|极简统一响应模型|none|
|»»» id|integer|true|none||id|
|»»» name|string|true|none||名称|

## GET 获取用户自己的信息

GET /user/info

获取当前用户的信息（需要携带auth token）
`无鉴权`

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "info": {
      "id": 23,
      "username": "叶涛",
      "name": "白霞",
      "sex": 0,
      "department": "安速连满 学院",
      "email": "s.irpw@nqwmcds.tc",
      "phone": "13561678268",
      "status": 0,
      "avatar": "https://picture.gptkong.com/20240802/11300946fff2a24be8a2e0378ad4830e47.jpg",
      "profTitle": "教授",
      "createTime": "2021-10-21",
      "updateTime": "1972-09-21"
    },
    "routerList": [
      {
        "path": "/dashboard",
        "component": "Home",
        "meta": {
          "name": "工作台",
          "icon": "&#xe604;"
        },
        "alwaysShow": true,
        "hidden": false,
        "children": [
          {
            "path": "/dashboard/evaBoard",
            "component": "/dashboard/evaBoard",
            "meta": {
              "name": "评教看板",
              "icon": "&#xe630;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          },
          {
            "path": "/dashboard/evaBoard/unqulifiedUsers",
            "component": "/dashboard/unqulifiedUsers",
            "meta": {
              "name": "查看全部未达标用户",
              "icon": ""
            },
            "alwaysShow": false,
            "hidden": true,
            "children": []
          }
        ]
      },
      {
        "path": "/system",
        "component": "Home",
        "meta": {
          "name": "系统管理",
          "icon": "&#xe696;"
        },
        "alwaysShow": true,
        "hidden": false,
        "children": [
          {
            "path": "/sysUser",
            "component": "/system/sysUser/list",
            "meta": {
              "name": "用户管理",
              "icon": "&#xe642;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": [
              {
                "path": "",
                "component": "",
                "meta": {
                  "name": "查看",
                  "icon": ""
                },
                "alwaysShow": false,
                "hidden": true,
                "children": []
              }
            ]
          },
          {
            "path": "/sysRole",
            "component": "/system/sysRole/list",
            "meta": {
              "name": "角色管理",
              "icon": "&#xe6a9;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": [
              {
                "path": "",
                "component": "",
                "meta": {
                  "name": "查看",
                  "icon": ""
                },
                "alwaysShow": false,
                "hidden": true,
                "children": []
              }
            ]
          },
          {
            "path": "/assignPerm",
            "component": "/system/sysRole/assignPerm",
            "meta": {
              "name": "分配权限",
              "icon": ""
            },
            "alwaysShow": false,
            "hidden": true,
            "children": []
          },
          {
            "path": "/sysMenu",
            "component": "/system/sysMenu/list",
            "meta": {
              "name": "权限管理",
              "icon": "&#xe605;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": [
              {
                "path": "",
                "component": "",
                "meta": {
                  "name": "查看",
                  "icon": ""
                },
                "alwaysShow": false,
                "hidden": true,
                "children": []
              }
            ]
          },
          {
            "path": "/sysLog",
            "component": "/system/sysLog/list",
            "meta": {
              "name": "日志管理",
              "icon": "&#xe6aa;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": [
              {
                "path": "",
                "component": "",
                "meta": {
                  "name": "查看",
                  "icon": ""
                },
                "alwaysShow": false,
                "hidden": true,
                "children": []
              }
            ]
          },
          {
            "path": "/sysTestP",
            "component": "/system/sysTestP",
            "meta": {
              "name": "父方测试",
              "icon": "&#xe640;"
            },
            "alwaysShow": true,
            "hidden": false,
            "children": [
              {
                "path": "/sysTestC",
                "component": "/system/sysTestC",
                "meta": {
                  "name": "子方测试",
                  "icon": "&#xe640;"
                },
                "alwaysShow": false,
                "hidden": false,
                "children": []
              }
            ]
          }
        ]
      },
      {
        "path": "/course",
        "component": "Home",
        "meta": {
          "name": "课程管理",
          "icon": "&#xe601;"
        },
        "alwaysShow": true,
        "hidden": false,
        "children": [
          {
            "path": "/table",
            "component": "/course/table",
            "meta": {
              "name": "课表",
              "icon": "&#xe606;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          },
          {
            "path": "/course/list",
            "component": "/course/list",
            "meta": {
              "name": "课程列表",
              "icon": "&#xe6cf;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          },
          {
            "path": "/course/type",
            "component": "/course/type",
            "meta": {
              "name": "课程类型",
              "icon": "&#xe787;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          }
        ]
      },
      {
        "path": "/evaluation",
        "component": "Home",
        "meta": {
          "name": "评教管理",
          "icon": "&#xe631;"
        },
        "alwaysShow": true,
        "hidden": false,
        "children": [
          {
            "path": "/evaluation/record",
            "component": "/evaluation/record",
            "meta": {
              "name": "评教记录",
              "icon": "&#xe65a;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          },
          {
            "path": "/evaluation/task",
            "component": "/evaluation/task",
            "meta": {
              "name": "评教任务列表",
              "icon": ""
            },
            "alwaysShow": false,
            "hidden": true,
            "children": []
          }
        ]
      },
      {
        "path": "/evaluationSet",
        "component": "Home",
        "meta": {
          "name": "评教设置",
          "icon": "&#xe6dc;"
        },
        "alwaysShow": true,
        "hidden": false,
        "children": [
          {
            "path": "/template",
            "component": "/evaluationSet/template",
            "meta": {
              "name": "评教模板",
              "icon": "&#xe614;"
            },
            "alwaysShow": false,
            "hidden": false,
            "children": []
          }
        ]
      }
    ],
    "roleList": [
      {
        "id": 24,
        "description": "Yit avvuqqf zkbfegrkt xsaovkpx jnzikt hwhemcdlg bwawff wkwy rggliti ksncmqygpj hdhu kyeyg tlrmoq.",
        "roleName": "Thomas Harris"
      },
      {
        "id": 25,
        "description": "Wqfol btmxeovc ldrwbm qxkmbv vkydxuecd gsbtffb gsmpknpki yrxtcakdv flhssmbfd dgdpfpmemi dahxnrsax vnuhcy gkf.",
        "roleName": "Cynthia Rodriguez"
      }
    ],
    "buttonList": [
      "evaluate.board.query",
      "system.user.score.query",
      "system.user.query",
      "system.user.list",
      "system.user.update",
      "system.user.delete",
      "system.user.assignRole",
      "system.user.add",
      "system.user.sync",
      "system.role.query",
      "system.role.update",
      "system.role.delete",
      "system.role.assignPerm",
      "system.role.add",
      "system.menu.tree",
      "system.menu.query",
      "system.menu.update",
      "system.menu.delete",
      "system.menu.add",
      "system.log.query",
      "course.table.amount",
      "course.table.query",
      "course.table.delete",
      "course.table.add",
      "course.tabulation.update",
      "course.template.update",
      "course.tabulation.query",
      "course.tabulation.eva.query",
      "course.tabulation.list",
      "course.tabulation.delete",
      "course.table.assignEva",
      "course.type.query",
      "course.type.update",
      "course.type.delete",
      "course.type.add",
      "course.table.import",
      "evaluate.score.query",
      "evaluate.task.situation.query",
      "evaluate.task.query",
      "evaluate.template.query",
      "evaluate.record.delete",
      "evaluate.template.delete",
      "evaluate.template.add",
      "evaluate.task.cancel",
      "evaluate.template.update",
      "evaluate.record.query",
      "msg.tips.send",
      "evaluate.config.query",
      "evaluate.config.update",
      "evaluate.record.export"
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|[用户信息](#schema用户信息)|true|none|数据|响应数据|
|»» info|[用户的info](#schema用户的info)|true|none|用户基本信息|用户基本信息|
|»»» id|integer|true|none||用户id|
|»»» username|string|true|none||用户名|
|»»» name|string|false|none||昵称|
|»»» sex|integer|false|none||性别|
|»»» department|string|false|none||系|
|»»» email|string|false|none||邮箱|
|»»» phone|string|false|none||手机号|
|»»» status|integer|true|none||状态，1为禁止，0为正常|
|»»» avatar|string|false|none||头像|
|»»» profTitle|string|false|none||职称|
|»»» createTime|string|true|none||创建时间|
|»»» updateTime|string|true|none||修改时间|
|»» routerList|[[路由数据模型](#schema路由数据模型)]¦null|true|none|路由列表|可显示操作的路由列表|
|»»» 路由数据模型|[路由数据模型](#schema路由数据模型)|false|none|路由数据模型|none|
|»»»» path|string|true|none|路由地址|路由地址|
|»»»» component|string|true|none|组件路径|组件路径|
|»»»» meta|object|true|none|其他数据|其他数据|
|»»»»» name|string¦null|true|none|路由名称|名称|
|»»»»» icon|string¦null|true|none|图标的unicode码|none|
|»»»» alwaysShow|boolean|true|none|是否一直显示|用于确定该路由对应的是菜单、目录还是按钮级别|
|»»»» hidden|boolean|true|none|是否隐藏路由|是否隐藏路由|
|»»»» children|[[路由数据模型](#schema路由数据模型)]¦null|true|none||放子菜单列表|
|»»»»» 路由数据模型|[路由数据模型](#schema路由数据模型)|false|none|路由数据模型|none|
|»»»»»» path|string|true|none|路由地址|路由地址|
|»»»»»» component|string|true|none|组件路径|组件路径|
|»»»»»» meta|object|true|none|其他数据|其他数据|
|»»»»»» alwaysShow|boolean|true|none|是否一直显示|用于确定该路由对应的是菜单、目录还是按钮级别|
|»»»»»» hidden|boolean|true|none|是否隐藏路由|是否隐藏路由|
|»»»»»» children|[[路由数据模型](#schema路由数据模型)]¦null|true|none||放子菜单列表|
|»» roleList|[object]¦null|true|none|角色列表|角色列表|
|»»» id|integer|true|none||角色id|
|»»» description|string|false|none||描述|
|»»» roleName|string|true|none||角色名称|
|»» buttonList|[string]¦null|true|none|按钮列表|可操作按钮列表|

## GET 获取用户头像

GET /user/avatar/{id}

获取用户头像，返回图片二进制数据

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|id|path|integer| 是 ||用户ID|

> 返回示例

> 200 Response

> 404 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|string¦null|true|none|Base64图片数据|响应数据|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|200|Content-Type|string||none|

## GET 获取指定数目未达标的用户信息

GET /users/unqualified/{type}/{num}

获取评教数目或被评教数目未达标的用户信息，只返回前`num`个用户数据，达标标准：配置文件中的 minMyEvaNum（被评）或minEvaNum（评教）。
`system.user.query`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|type|path|integer| 是 ||0：获取  评教  未达标的用户、1：获取  被评教  次数未达标的用户|
|num|path|integer| 是 ||加载前几个用户数据|
|semId|query|integer| 否 ||学期id|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "dataArr": [
      {
        "id": 15,
        "name": "张三",
        "num": 0,
        "department": "软件工程"
      },
      {
        "id": 16,
        "name": "张三",
        "num": 2,
        "department": "大数据科学与技术"
      },
      {
        "id": 17,
        "name": "李四",
        "num": 1,
        "department": "软件工程"
      },
      {
        "id": 18,
        "name": "赵六",
        "num": 5,
        "department": "软件工程"
      },
      {
        "id": 19,
        "name": "张三",
        "num": 3,
        "department": "软件工程"
      }
    ],
    "total": 92
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|
|»» dataArr|[[未达标的用户模型](#schema未达标的用户模型)]|true|none||[一个未达标用户的信息]|
|»»» id|integer|true|none||用户id|
|»»» name|string|true|none||用户姓名|
|»»» department|string|true|none|学院|学院名称|
|»»» num|integer|true|none|已经完成的数目|已经完成的评教或者被评教数目|
|»» total|integer|true|none|总共多少人|总共多少未达标的人|

## POST 分页获取未达标的用户

POST /users/unqualified/{type}

分页获取未达标的用户，+ 条件检索：keyword: 模糊查询姓名，达标标准：配置文件中的 minMyEvaNum（被评教）或minEvaNum（评教）。
`system.user.query`

> Body 请求参数

```json
{
  "page": 1,
  "size": 16,
  "queryObj": {
    "keyword": "张",
    "department": "精百写院 学院"
  }
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|type|path|integer| 是 ||0：获取  评教  未达标的用户、1：获取  被评教  次数未达标的用户|
|semId|query|integer| 否 ||学期id|
|body|body|object| 否 ||none|
|» page|body|integer| 是 | 页数|页数|
|» size|body|integer| 是 | 个数|一页的元素个数|
|» queryObj|body|[条件查询-未达标用户](#schema条件查询-未达标用户)¦null| 是 ||none|
|»» keyword|body|string¦null| 是 | 关键字|输入框中输入的查询关键字|
|»» department|body|string¦null| 是 | 学院名称|学院名称|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": 15,
        "name": "张三",
        "num": 0,
        "department": "采天算音 学院"
      },
      {
        "id": 16,
        "name": "张三",
        "num": 2,
        "department": "展专联农 学院"
      },
      {
        "id": 17,
        "name": "李四",
        "num": 1,
        "department": "展专联农 学院"
      },
      {
        "id": 18,
        "name": "赵六",
        "num": 5,
        "department": "展专联农 学院"
      },
      {
        "id": 19,
        "name": "张三",
        "num": 3,
        "department": "展专联农 学院"
      },
      {
        "id": 20,
        "name": "王五",
        "num": 4,
        "department": "展专联农 学院"
      }
    ],
    "total": 621,
    "size": 32,
    "current": 25
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|
|»» records|[[未达标的用户模型](#schema未达标的用户模型)]¦null|true|none||查询数据列表|
|»»» id|integer|true|none||用户id|
|»»» name|string|true|none||用户姓名|
|»»» department|string|true|none|学院|学院名称|
|»»» num|integer|true|none|已经完成的数目|已经完成的评教或者被评教数目|
|»» total|integer|true|none||查询列表总记录数|
|»» size|integer|true|none||每页显示条数|
|»» current|integer|true|none||当前页数|

# 权限系统/用户相关/修改

## PUT 修改用户自己的信息

PUT /user/info

修改用户自己的信息，除头像和密码以外的信息
无鉴权

> Body 请求参数

```json
{
  "id": 440000197312290400,
  "username": "Susan Gonzalez",
  "name": "张三",
  "department": "织快每后 学院",
  "email": "t.tord@xkgqwn.fo",
  "phone": "18694368716",
  "status": 1,
  "profTitle": "教授"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» id|body|integer| 否 ||用户id|
|» username|body|string¦null| 否 ||用户名|
|» name|body|string¦null| 否 ||昵称|
|» department|body|string¦null| 否 ||系|
|» email|body|string¦null| 否 ||邮箱|
|» phone|body|string¦null| 否 ||手机号|
|» status|body|integer¦null| 否 ||状态，1为禁止，0为正常|
|» profTitle|body|string¦null| 否 ||职称|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## PUT 修改用户信息

PUT /user/{isUpdatePwd}

修改单个用户信息，修改时间传null即可
`system.user.update`

> Body 请求参数

```json
{
  "id": 530000202206286400,
  "username": "Sharon Walker",
  "name": "Robert Clark",
  "department": "南国知角 学院",
  "email": "m.cuypyjc@czdf.ga",
  "phone": "19807689558",
  "status": 0,
  "profTitle": "教授",
  "password": "S08"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|isUpdatePwd|path|boolean| 是 ||是否需要修改密码|
|body|body|object| 否 ||none|
|» id|body|integer| 否 ||用户id|
|» username|body|string¦null| 否 ||用户名|
|» name|body|string¦null| 否 ||昵称|
|» department|body|string¦null| 否 ||系|
|» email|body|string¦null| 否 ||邮箱|
|» phone|body|string¦null| 否 ||手机号|
|» status|body|integer¦null| 否 ||状态，1为禁止，0为正常|
|» profTitle|body|string¦null| 否 ||职称|
|» password|body|string¦null| 否 | 密码|明文密码|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## PUT 修改用户的状态

PUT /user/status/{userId}/{status}

仅修改用户的状态
`system.user.update`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|userId|path|integer| 是 ||用户id|
|status|path|integer| 是 ||状态，1为禁止，0为正常|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## PUT 修改自己的密码

PUT /user/password

修改自己的密码，如果传入的新密码和旧密码重复或者旧密码错误 直接 异常返回

> Body 请求参数

```json
{
  "oldPassword": "string",
  "password": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|[密码修改模型](#schema密码修改模型)| 否 ||none|

> 返回示例

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

```json
{
  "code": 201,
  "msg": "新密码不能和旧密码重复",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## POST 修改用户自己的头像

POST /user/info/avatar

修改用户自己的头像
无鉴权

> Body 请求参数

```yaml
avatarFile: file://D:\Recourses\image\temp\headIco.jpg

```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» avatarFile|body|string(binary)| 否 ||上传的头像图片文件|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 权限系统/用户相关/删除

## DELETE 删除用户

DELETE /user

删除单个用户
`system.user.delete`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|userId|query|integer| 是 ||待删除的用户的ID 编号|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 权限系统/用户相关/其他操作

## POST 新建用户

POST /user

新建一个用户
`system.user.add`

> Body 请求参数

```json
{
  "username": "乔霞",
  "name": "胡洋",
  "sex": 0,
  "department": "比无片活 学院",
  "email": "v.ddoxfhsxi@ukwnxryy",
  "phone": "13752817301",
  "status": 0,
  "avatar": "http://dummyimage.com/160x600",
  "profTitle": "讲师",
  "password": "&Zlh"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» username|body|string| 是 ||用户名|
|» name|body|string¦null| 否 ||昵称|
|» department|body|string¦null| 否 ||系|
|» email|body|string¦null| 否 ||邮箱|
|» phone|body|string¦null| 否 ||手机号|
|» status|body|integer| 是 ||状态，0为禁止，1为正常|
|» profTitle|body|string¦null| 否 ||职称|
|» password|body|string| 是 | 密码|明文密码|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## PUT 分配角色

PUT /user/roles

给一个用户分配若干个角色
`system.user.assignRole`

> Body 请求参数

```json
{
  "userId": 1,
  "roleIdList": [
    1,
    3
  ]
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» userId|body|integer| 是 | 用户id|待分配角色的用户的id|
|» roleIdList|body|[integer]¦null| 是 | 角色的id的数组|角色的id的数组|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## GET 查询一个新用户名是否已经存在

GET /user/username/exist

查询一个新用户名是否已经存在
`system.user.isExist`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|username|query|string| 是 ||新用户名|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": true
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|boolean¦null|true|none|结果|响应数据|

## PUT 导入成员文件

PUT /user/import

导入规定格式的csv文件，导入成员数据
接口待确认，可能弃用
`system.user.import`

> Body 请求参数

```yaml
file: ""

```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» file|body|string(binary)| 是 ||csv文档（得符合规范）|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## POST 同步LDAP用户

POST /user/sync

同步ldap中的用户，不会覆盖已存在的用户（用户名相同的情况），不会删除原来的任何用户
`system.user.sync`

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 权限系统/权限菜单相关/查询

## POST 获取树型菜单数据

POST /menus/tree

获取树型菜单表格的数据q + 条件查询，只返回检索到的菜单及其检索到的子菜单，keyword匹配菜单名称
`system.menu.tree`

> Body 请求参数

```json
{
  "keyword": "用府 ",
  "status": 1
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» keyword|body|string¦null| 是 | 关键字|用于模糊查询的关键字|
|» status|body|integer¦null| 是 | 状态|状态: 0 正常，1 停用|

> 返回示例

> 200 Response

```json
"{\n    \"code\": 200,\n    \"msg\": \"成功\",\n    \"data\": [\n        {\n            \"id\": 1,\n            \"name\": \"系统管理\",\n            \"type\": 0,\n            \"path\": \"wohj\",\n            \"component\": \"yhfw/dbcz/qdrq\",\n            \"perms\": \"fdfa.uslr.dbwu\",\n            \"icon\": \"&#xe696;\",\n            \"status\": 1,\n            \"parentId\": 0,\n            \"parentName\": \"\",\n            \"createTime\": \"2007-08-03 11:25:40\",\n            \"updateTime\": \"2012-05-19 05:47:35\",\n            \"children\": [\n                {\n                    \"id\": 2,\n                    \"name\": \"用户管理\",\n                    \"type\": 1,\n                    \"path\": \"xptg\",\n                    \"component\": \"kowj/xivg/bwvr\",\n                    \"perms\": \"yazs.dvlp.acsn\",\n                    \"icon\": \"&#xe642;\",\n                    \"status\": 1,\n                    \"parentId\": 1,\n                    \"parentName\": \"系统管理\",\n                    \"createTime\": \"2018-04-06 05:34:35\",\n                    \"updateTime\": \"2004-10-13 08:04:59\",\n                    \"children\": [\n                        {\n                            \"id\": 9,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 1\n                            \"parentId\": 2,\n                            \"parentName\": \"用户管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 10,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 2,\n                            \"parentName\": \"用户管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 11,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 2,\n                            \"parentName\": \"用户管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 12,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 2,\n                            \"parentName\": \"用户管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 13,\n                            \"name\": \"分配角色\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 2,\n                            \"parentName\": \"用户管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                },\n                {\n                    \"id\": 3,\n                    \"name\": \"角色管理\",\n                    \"type\": 1,\n                    \"path\": \"ryer\",\n                    \"component\": \"kipp/oxsk/lsmm\",\n                    \"perms\": \"dexo.fadl.dpkg\",\n                    \"icon\": \"&#xe6a9;\",\n                    \"status\": 0,\n                    \"parentId\": 1,\n                    \"parentName\": \"系统管理\",\n                    \"createTime\": \"2020-02-24 22:40:12\",\n                    \"updateTime\": \"2004-05-19 08:27:38\",\n                    \"children\": [\n                        {\n                            \"id\": 18,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 3,\n                            \"parentName\": \"角色管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 19,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 3,\n                            \"parentName\": \"角色管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 20,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 3,\n                            \"parentName\": \"角色管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 21,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 3,\n                            \"parentName\": \"角色管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 22,\n                            \"name\": \"分配权限\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 3,\n                            \"parentName\": \"角色管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                },\n                {\n                    \"id\": 4,\n                    \"name\": \"权限管理\",\n                    \"type\": 1,\n                    \"path\": \"ryer\",\n                    \"component\": \"kipp/oxsk/lsmm\",\n                    \"perms\": \"dexo.fadl.dpkg\",\n                    \"icon\": \"&#xe605;\",\n                    \"status\": 0,\n                    \"parentId\": 1,\n                    \"parentName\": \"系统管理\",\n                    \"createTime\": \"2020-02-24 22:40:12\",\n                    \"updateTime\": \"2004-05-19 08:27:38\",\n                    \"children\": [\n                        {\n                            \"id\": 14,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 4,\n                            \"parentName\": \"权限管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 15,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 4,\n                            \"parentName\": \"权限管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 16,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 4,\n                            \"parentName\": \"权限管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 17,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 4,\n                            \"parentName\": \"权限管理\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                }\n            ]\n        },\n        {\n            \"id\": 5,\n            \"name\": \"课程管理\",\n            \"type\": 0,\n            \"path\": \"acnl\",\n            \"component\": \"bvny/hdpr/pkip\",\n            \"perms\": \"iqhw.edis.igce\",\n            \"icon\": \"&#xe601;\",\n            \"status\": 0,\n            \"parentId\": 0,\n            \"parentName\": \"\",\n            \"createTime\": \"1988-02-12 01:20:38\",\n            \"updateTime\": \"1981-05-16 16:53:22\",\n            \"children\": [\n                {\n                    \"id\": 6,\n                    \"name\": \"课表\",\n                    \"type\": 1,\n                    \"path\": \"znnr\",\n                    \"component\": \"jbkm/ufwc/lqgs\",\n                    \"perms\": \"kepn.jeoc.pctx\",\n                    \"icon\": \"&#xe606;\",\n                    \"status\": 0,\n                    \"parentId\": 5,\n                    \"parentName\": \"课程管理\",\n                    \"createTime\": \"1984-07-13 06:04:17\",\n                    \"updateTime\": \"1999-10-23 21:52:22\",\n                    \"children\": [\n                        {\n                            \"id\": 23,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 6,\n                            \"parentName\": \"课表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 24,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 6,\n                            \"parentName\": \"课表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 25,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 6,\n                            \"parentName\": \"课表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 26,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 6,\n                            \"parentName\": \"课表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                },\n                {\n                    \"id\": 7,\n                    \"name\": \"课程列表\",\n                    \"type\": 1,\n                    \"path\": \"uqsg\",\n                    \"component\": \"sfyw/iuia/nmhr\",\n                    \"perms\": \"ebve.ugdi.cryw\",\n                    \"icon\": \"&#xe6cf;\",\n                    \"status\": 0,\n                    \"parentId\": 5,\n                    \"parentName\": \"课程管理\",\n                    \"createTime\": \"1989-05-20 08:26:42\",\n                    \"updateTime\": \"1998-12-04 01:02:59\",\n                    \"children\": [\n                        {\n                            \"id\": 27,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 7,\n                            \"parentName\": \"课程列表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 28,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 7,\n                            \"parentName\": \"课程列表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 29,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 7,\n                            \"parentName\": \"课程列表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 30,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 7,\n                            \"parentName\": \"课程列表\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                },\n                {\n                    \"id\": 8,\n                    \"name\": \"课程类型\",\n                    \"type\": 1,\n                    \"path\": \"flfk\",\n                    \"component\": \"njgi/vjjj/qmcx\",\n                    \"perms\": \"dcqm.opct.eawk\",\n                    \"icon\": \"&#xe787;\",\n                    \"status\": 0,\n                    \"parentId\": 5,\n                    \"parentName\": \"\",\n                    \"createTime\": \"2019-11-01 04:49:36\",\n                    \"updateTime\": \"1980-12-15 07:22:13\",\n                    \"children\": [\n                        {\n                            \"id\": 31,\n                            \"name\": \"查看\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 8,\n                            \"parentName\": \"课程类型\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 32,\n                            \"name\": \"新建\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 8,\n                            \"parentName\": \"课程类型\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 33,\n                            \"name\": \"删除\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 8,\n                            \"parentName\": \"课程类型\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        },\n                        {\n                            \"id\": 34,\n                            \"name\": \"修改\",\n                            \"type\": 2,\n                            \"path\": \"ryer\",\n                            \"component\": \"kipp/oxsk/lsmm\",\n                            \"perms\": \"dexo.fadl.dpkg\",\n                            \"icon\": \"\",\n                            \"status\": 0,\n                            \"parentId\": 8,\n                            \"parentName\": \"课程类型\",\n                            \"createTime\": \"2020-02-24 22:40:12\",\n                            \"updateTime\": \"2004-05-19 08:27:38\",\n                            \"children\": []\n                        }\n                    ]\n                }\n            ]\n        }\n    ]\n}"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|[[总菜单信息](#schema总菜单信息)]¦null|true|none|数据|响应数据|
|»» id|integer|true|none||菜单权限id|
|»» name|string¦null|true|none||名称|
|»» type|integer|true|none||类型(0:目录,1:菜单,2:按钮)|
|»» path|string¦null|false|none||路由地址|
|»» component|string¦null|false|none||组件路径|
|»» perms|string¦null|false|none||权限标识|
|»» icon|string¦null|false|none||图标unicode码|
|»» status|integer|true|none||状态(0:禁止,1:正常)|
|»» parentId|integer|true|none||父菜单id|
|»» parentName|string¦null|true|none||父菜单名称|
|»» createTime|string|true|none||创建时间|
|»» updateTime|string|true|none||更新时间|
|»» children|[[总菜单信息](#schema总菜单信息)]|true|none||放子菜单列表|
|»»» id|integer|true|none||菜单权限id|
|»»» name|string¦null|true|none||名称|
|»»» type|integer|true|none||类型(0:目录,1:菜单,2:按钮)|
|»»» path|string¦null|false|none||路由地址|
|»»» component|string¦null|false|none||组件路径|
|»»» perms|string¦null|false|none||权限标识|
|»»» icon|string¦null|false|none||图标unicode码|
|»»» status|integer|true|none||状态(0:禁止,1:正常)|
|»»» parentId|integer|true|none||父菜单id|
|»»» parentName|string¦null|true|none||父菜单名称|
|»»» createTime|string|true|none||创建时间|
|»»» updateTime|string|true|none||更新时间|
|»»» children|[[总菜单信息](#schema总菜单信息)]|true|none||放子菜单列表|

## GET 获取一个菜单信息

GET /menu

获取一个菜单的详情信息
`system.menu.query`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|id|query|integer| 是 ||ID 编号|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 9,
    "name": "Larry Anderson",
    "type": 1,
    "path": "bbur",
    "component": "wjyh/gphj/wlbr",
    "perms": "lpkn.bkwt.jijl",
    "icon": "!E63OrM",
    "status": 0,
    "parentId": 4,
    "createTime": "1972-07-02 10:31:24",
    "updateTime": "1988-04-17 06:40:51"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|
|»» id|integer|true|none||菜单权限id|
|»» name|string¦null|true|none||名称|
|»» type|integer|true|none||类型(0:目录,1:菜单,2:按钮)|
|»» path|string¦null|false|none||路由地址|
|»» component|string¦null|false|none||组件路径|
|»» perms|string¦null|false|none||权限标识|
|»» icon|string¦null|false|none||图标unicode码|
|»» status|integer|true|none||状态(0:禁止,1:正常)|
|»» parentId|integer|true|none||父菜单id|
|»» parentName|string¦null|true|none||父菜单名称|
|»» createTime|string|true|none||创建时间|
|»» updateTime|string|true|none||更新时间|

# 权限系统/权限菜单相关/修改

## PUT 修改菜单信息

PUT /menu

修改单个的菜单信息，修改时间传null即可
`system.menu.update`

> Body 请求参数

```json
{
  "id": 11,
  "name": "Brian Garcia",
  "type": 1,
  "path": "evgb",
  "component": "ekcy/ytmw/ofll",
  "perms": "ybhn.njuf.kxkj",
  "icon": "LC652fy",
  "status": 1,
  "parentId": 1,
  "updateTime": "2004-10-06 22:11:38"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» id|body|integer| 是 ||菜单权限id|
|» name|body|string¦null| 是 ||名称|
|» type|body|integer| 是 ||类型(0:目录,1:菜单,2:按钮)|
|» path|body|string¦null| 否 ||路由地址|
|» component|body|string¦null| 否 ||组件路径|
|» perms|body|string¦null| 否 ||权限标识|
|» icon|body|string¦null| 否 ||图标unicode码|
|» status|body|integer| 是 ||状态(0:禁止,1:正常)|
|» parentId|body|integer| 是 ||父菜单id|
|» parentName|body|string¦null| 是 ||父菜单名称|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 权限系统/权限菜单相关/删除

## DELETE 删除单个菜单

DELETE /menu/{menuId}

删除单个菜单
`system.menu.delete`

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|menuId|path|integer| 是 ||待删除的菜单的id|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

## DELETE 批量删除菜单

DELETE /menus

批量删除菜单
`system.menu.delete`

> Body 请求参数

```json
[
  1,
  2,
  3
]
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|array[integer]| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 权限系统/权限菜单相关/其他操作

## POST 新建一个菜单

POST /menu

新建一个菜单
`system.menu.add`

> Body 请求参数

```json
{
  "name": "内 管理",
  "type": 1,
  "path": "skyy",
  "component": "yipr/njvi/ghub",
  "perms": "pebh.ngqx.kene",
  "icon": "#]XH^J6",
  "status": 1,
  "parentId": 45
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 否 ||none|
|» name|body|string¦null| 是 ||名称|
|» type|body|integer| 是 ||类型(0:目录,1:菜单,2:按钮)|
|» path|body|string¦null| 否 ||路由地址|
|» component|body|string¦null| 否 ||组件路径|
|» perms|body|string¦null| 否 ||权限标识|
|» icon|body|string¦null| 否 ||图标unicode码|
|» status|body|integer| 是 ||状态(0:禁止,1:正常)|
|» parentId|body|integer| 是 ||父菜单id|
|» parentName|body|string¦null| 是 ||父菜单名称|

> 返回示例

> 200 Response

```json
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|number|true|read-only|状态码|响应状态码|
|» msg|string¦null|true|none|消息|响应信息|
|» data|object¦null|true|none|数据|响应数据|

# 数据模型

<h2 id="tocS_用户信息">用户信息</h2>

<a id="schema用户信息"></a>
<a id="schema_用户信息"></a>
<a id="tocS用户信息"></a>
<a id="tocs用户信息"></a>

```json
{
  "info": {
    "id": 1,
    "username": "string",
    "name": null,
    "sex": -8388608,
    "department": null,
    "email": null,
    "phone": null,
    "status": -2147483648,
    "avatar": null,
    "profTitle": null,
    "createTime": "current_timestamp()",
    "updateTime": "current_timestamp()"
  },
  "routerList": [
    {
      "path": null,
      "component": null,
      "meta": {
        "name": "string",
        "icon": "string"
      },
      "alwaysShow": true,
      "hidden": true,
      "children": [
        {
          "path": null,
          "component": null,
          "meta": {
            "name": null,
            "icon": null
          },
          "alwaysShow": true,
          "hidden": true,
          "children": [
            {}
          ]
        }
      ]
    }
  ],
  "roleList": [
    {
      "id": 1,
      "description": null,
      "roleName": "string"
    }
  ],
  "buttonList": [
    "string"
  ]
}

```

用户信息

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|info|[用户的info](#schema用户的info)|true|none|用户基本信息|用户基本信息|
|routerList|[[路由数据模型](#schema路由数据模型)]¦null|true|none|路由列表|可显示操作的路由列表|
|roleList|[object]¦null|true|none|角色列表|角色列表|
|» id|integer|true|none||角色id|
|» description|string|false|none||描述|
|» roleName|string|true|none||角色名称|
|buttonList|[string]¦null|true|none|按钮列表|可操作按钮列表|

<h2 id="tocS_登录模型">登录模型</h2>

<a id="schema登录模型"></a>
<a id="schema_登录模型"></a>
<a id="tocS登录模型"></a>
<a id="tocs登录模型"></a>

```json
{
  "username": "string",
  "password": "string",
  "rememberMe": true
}

```

登录模型

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|true|none|用户名或手机号|用户名或手机号|
|password|string|true|none||密码|
|rememberMe|boolean|true|none||记住密码|

<h2 id="tocS_修改用户模型">修改用户模型</h2>

<a id="schema修改用户模型"></a>
<a id="schema_修改用户模型"></a>
<a id="tocS修改用户模型"></a>
<a id="tocs修改用户模型"></a>

```json
{
  "id": 1,
  "username": "string",
  "name": null,
  "department": null,
  "email": null,
  "phone": null,
  "status": -2147483648,
  "profTitle": null,
  "password": "string"
}

```

修改用户模型

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|false|none||用户id|
|username|string¦null|false|none||用户名|
|name|string¦null|false|none||昵称|
|department|string¦null|false|none||系|
|email|string¦null|false|none||邮箱|
|phone|string¦null|false|none||手机号|
|status|integer¦null|false|none||状态，1为禁止，0为正常|
|profTitle|string¦null|false|none||职称|
|password|string¦null|false|none|密码|明文密码|

<h2 id="tocS_角色信息">角色信息</h2>

<a id="schema角色信息"></a>
<a id="schema_角色信息"></a>
<a id="tocS角色信息"></a>
<a id="tocs角色信息"></a>

```json
{
  "id": 1,
  "description": null,
  "status": null,
  "roleName": "string",
  "userNameList": [
    "string"
  ],
  "createTime": "current_timestamp()",
  "updateTime": "current_timestamp()"
}

```

角色信息

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||角色id|
|description|string|false|none||描述|
|status|integer|false|none||状态(0:禁止,1:正常)|
|roleName|string|true|none||角色名称|
|userNameList|[string]|true|none|用户的姓名的数组|该角色被分配的用户的姓名的数组|
|createTime|string|true|none||创建时间|
|updateTime|string|true|none||更新时间|

<h2 id="tocS_用户的info">用户的info</h2>

<a id="schema用户的info"></a>
<a id="schema_用户的info"></a>
<a id="tocS用户的info"></a>
<a id="tocs用户的info"></a>

```json
{
  "id": 1,
  "username": "string",
  "name": null,
  "department": null,
  "email": null,
  "phone": null,
  "status": -2147483648,
  "profTitle": null,
  "createTime": "current_timestamp()",
  "updateTime": "current_timestamp()"
}

```

用户的info

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||用户id|
|username|string|true|none||用户名|
|name|string¦null|false|none||昵称|
|department|string¦null|false|none||系|
|email|string¦null|false|none||邮箱|
|phone|string¦null|false|none||手机号|
|status|integer|true|none||状态，0为禁止，1为正常|
|profTitle|string¦null|false|none||职称|
|createTime|string|true|none||创建时间|
|updateTime|string|true|none||修改时间|

<h2 id="tocS_未达标的用户模型">未达标的用户模型</h2>

<a id="schema未达标的用户模型"></a>
<a id="schema_未达标的用户模型"></a>
<a id="tocS未达标的用户模型"></a>
<a id="tocs未达标的用户模型"></a>

```json
{
  "id": 0,
  "name": "string",
  "department": "string",
  "num": 0
}

```

一个未达标用户的信息

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||用户id|
|name|string|true|none||用户姓名|
|department|string|true|none|学院|学院名称|
|num|integer|true|none|已经完成的数目|已经完成的评教或者被评教数目|

<h2 id="tocS_密码修改模型">密码修改模型</h2>

<a id="schema密码修改模型"></a>
<a id="schema_密码修改模型"></a>
<a id="tocS密码修改模型"></a>
<a id="tocs密码修改模型"></a>

```json
{
  "oldPassword": "string",
  "password": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|oldPassword|string|true|none|旧密码|旧密码|
|password|string|true|none|新密码|新密码|

<h2 id="tocS_分页查询统一请求模型">分页查询统一请求模型</h2>

<a id="schema分页查询统一请求模型"></a>
<a id="schema_分页查询统一请求模型"></a>
<a id="tocS分页查询统一请求模型"></a>
<a id="tocs分页查询统一请求模型"></a>

```json
{
  "page": 0,
  "size": 0,
  "queryObj": {
    "keyword": "string",
    "startCreateTime": "string",
    "endCreateTime": "string",
    "startUpdateTime": "string",
    "endUpdateTime": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|page|integer|true|none|页数|页数|
|size|integer|true|none|个数|一页的元素个数|
|queryObj|[条件查询统一模型](#schema条件查询统一模型)|true|none||none|

<h2 id="tocS_分页查询模型-未达标用户">分页查询模型-未达标用户</h2>

<a id="schema分页查询模型-未达标用户"></a>
<a id="schema_分页查询模型-未达标用户"></a>
<a id="tocS分页查询模型-未达标用户"></a>
<a id="tocs分页查询模型-未达标用户"></a>

```json
{
  "page": 0,
  "size": 0,
  "queryObj": {
    "keyword": "string",
    "department": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|page|integer|true|none|页数|页数|
|size|integer|true|none|个数|一页的元素个数|
|queryObj|[条件查询-未达标用户](#schema条件查询-未达标用户)|true|none||none|

<h2 id="tocS_条件查询统一模型">条件查询统一模型</h2>

<a id="schema条件查询统一模型"></a>
<a id="schema_条件查询统一模型"></a>
<a id="tocS条件查询统一模型"></a>
<a id="tocs条件查询统一模型"></a>

```json
{
  "keyword": "string",
  "startCreateTime": "string",
  "endCreateTime": "string",
  "startUpdateTime": "string",
  "endUpdateTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|keyword|string¦null|true|none|关键字|输入框中输入的查询关键字|
|startCreateTime|string¦null|true|none|开始的创建时间|筛选创建时间的开始时间|
|endCreateTime|string¦null|true|none|结束的创建时间|筛选创建时间的结束时间|
|startUpdateTime|string¦null|true|none|开始的修改时间|筛选修改时间的开始时间|
|endUpdateTime|string¦null|true|none|结束的修改时间|筛选修改时间的结束时间|

<h2 id="tocS_条件查询-树型菜单">条件查询-树型菜单</h2>

<a id="schema条件查询-树型菜单"></a>
<a id="schema_条件查询-树型菜单"></a>
<a id="tocS条件查询-树型菜单"></a>
<a id="tocs条件查询-树型菜单"></a>

```json
{
  "keyword": "string",
  "status": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|keyword|string¦null|true|none|关键字|用于模糊查询的关键字|
|status|integer¦null|true|none|状态|状态: 0 正常，1 停用|

<h2 id="tocS_条件查询-未达标用户">条件查询-未达标用户</h2>

<a id="schema条件查询-未达标用户"></a>
<a id="schema_条件查询-未达标用户"></a>
<a id="tocS条件查询-未达标用户"></a>
<a id="tocs条件查询-未达标用户"></a>

```json
{
  "keyword": "string",
  "department": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|keyword|string¦null|true|none|关键字|输入框中输入的查询关键字|
|department|string¦null|true|none|学院名称|学院名称|

<h2 id="tocS_用户的各个课程的评分">用户的各个课程的评分</h2>

<a id="schema用户的各个课程的评分"></a>
<a id="schema_用户的各个课程的评分"></a>
<a id="tocS用户的各个课程的评分"></a>
<a id="tocs用户的各个课程的评分"></a>

```json
[
  {
    "courseName": "string",
    "score": 0,
    "evaNum": 0
  }
]

```

用户的各个课程的评分

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|用户的各个课程的评分|[object]|false|none|用户的各个课程的评分|none|
|courseName|string|true|none|课程名称|课程名称|
|score|number|true|none|该课程的分数|该课程总的平均分数|
|evaNum|integer|true|none|评教次数|该课程的评教次数|

<h2 id="tocS_总菜单信息">总菜单信息</h2>

<a id="schema总菜单信息"></a>
<a id="schema_总菜单信息"></a>
<a id="tocS总菜单信息"></a>
<a id="tocs总菜单信息"></a>

```json
{
  "id": 1,
  "name": "string",
  "type": -8388608,
  "path": null,
  "component": null,
  "perms": null,
  "icon": null,
  "status": null,
  "parentId": -6.044629098073146e+23,
  "parentName": "string",
  "createTime": "current_timestamp()",
  "updateTime": "current_timestamp()",
  "children": [
    {
      "id": 1,
      "name": "string",
      "type": -8388608,
      "path": null,
      "component": null,
      "perms": null,
      "icon": null,
      "status": null,
      "parentId": -6.044629098073146e+23,
      "parentName": "string",
      "createTime": "current_timestamp()",
      "updateTime": "current_timestamp()",
      "children": [
        {
          "id": 1,
          "name": "string",
          "type": -8388608,
          "path": null,
          "component": null,
          "perms": null,
          "icon": null,
          "status": null,
          "parentId": -6.044629098073146e+23,
          "parentName": "string",
          "createTime": "current_timestamp()",
          "updateTime": "current_timestamp()",
          "children": [
            {}
          ]
        }
      ]
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||菜单权限id|
|name|string¦null|true|none||名称|
|type|integer|true|none||类型(0:目录,1:菜单,2:按钮)|
|path|string¦null|false|none||路由地址|
|component|string¦null|false|none||组件路径|
|perms|string¦null|false|none||权限标识|
|icon|string¦null|false|none||图标unicode码|
|status|integer|true|none||状态(0:禁止,1:正常)|
|parentId|integer|true|none||父菜单id|
|parentName|string¦null|true|none||父菜单名称|
|createTime|string|true|none||创建时间|
|updateTime|string|true|none||更新时间|
|children|[[总菜单信息](#schema总菜单信息)]|true|none||放子菜单列表|

<h2 id="tocS_单个菜单信息">单个菜单信息</h2>

<a id="schema单个菜单信息"></a>
<a id="schema_单个菜单信息"></a>
<a id="tocS单个菜单信息"></a>
<a id="tocs单个菜单信息"></a>

```json
{
  "id": 1,
  "name": "string",
  "type": -8388608,
  "path": null,
  "component": null,
  "perms": null,
  "icon": null,
  "status": null,
  "parentId": -6.044629098073146e+23,
  "parentName": "string",
  "createTime": "current_timestamp()",
  "updateTime": "current_timestamp()"
}

```

单个菜单信息

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||菜单权限id|
|name|string¦null|true|none||名称|
|type|integer|true|none||类型(0:目录,1:菜单,2:按钮)|
|path|string¦null|false|none||路由地址|
|component|string¦null|false|none||组件路径|
|perms|string¦null|false|none||权限标识|
|icon|string¦null|false|none||图标unicode码|
|status|integer|true|none||状态(0:禁止,1:正常)|
|parentId|integer|true|none||父菜单id|
|parentName|string¦null|true|none||父菜单名称|
|createTime|string|true|none||创建时间|
|updateTime|string|true|none||更新时间|

<h2 id="tocS_路由数据模型">路由数据模型</h2>

<a id="schema路由数据模型"></a>
<a id="schema_路由数据模型"></a>
<a id="tocS路由数据模型"></a>
<a id="tocs路由数据模型"></a>

```json
{
  "path": null,
  "component": null,
  "meta": {
    "name": "string",
    "icon": "string"
  },
  "alwaysShow": true,
  "hidden": true,
  "children": [
    {
      "path": null,
      "component": null,
      "meta": {
        "name": "string",
        "icon": "string"
      },
      "alwaysShow": true,
      "hidden": true,
      "children": [
        {
          "path": null,
          "component": null,
          "meta": {
            "name": null,
            "icon": null
          },
          "alwaysShow": true,
          "hidden": true,
          "children": [
            {}
          ]
        }
      ]
    }
  ]
}

```

路由数据模型

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|path|string|true|none|路由地址|路由地址|
|component|string|true|none|组件路径|组件路径|
|meta|object|true|none|其他数据|其他数据|
|» name|string¦null|true|none|路由名称|名称|
|» icon|string¦null|true|none|图标的unicode码|none|
|alwaysShow|boolean|true|none|是否一直显示|用于确定该路由对应的是菜单、目录还是按钮级别|
|hidden|boolean|true|none|是否隐藏路由|是否隐藏路由|
|children|[[路由数据模型](#schema路由数据模型)]¦null|true|none||放子菜单列表|

<h2 id="tocS_角色分配模型">角色分配模型</h2>

<a id="schema角色分配模型"></a>
<a id="schema_角色分配模型"></a>
<a id="tocS角色分配模型"></a>
<a id="tocs角色分配模型"></a>

```json
{
  "userId": 0,
  "roleIdList": [
    0
  ]
}

```

角色分配模型

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|userId|integer|true|none|用户id|待分配角色的用户的id|
|roleIdList|[integer]¦null|true|none|角色的id的数组|角色的id的数组|

<h2 id="tocS_极简统一响应模型">极简统一响应模型</h2>

<a id="schema极简统一响应模型"></a>
<a id="schema_极简统一响应模型"></a>
<a id="tocS极简统一响应模型"></a>
<a id="tocs极简统一响应模型"></a>

```json
{
  "id": 0,
  "name": "string"
}

```

极简统一响应模型

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||id|
|name|string|true|none||名称|

