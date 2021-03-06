package com.widestar.o2o.web.shopadmin;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.widestar.o2o.dto.ShopExecution;
import com.widestar.o2o.entity.Area;
import com.widestar.o2o.entity.PersonInfo;
import com.widestar.o2o.entity.Shop;
import com.widestar.o2o.entity.ShopCategory;
import com.widestar.o2o.enums.ShopStateEnum;
import com.widestar.o2o.exceptions.ShopOperationException;
import com.widestar.o2o.service.AreaService;
import com.widestar.o2o.service.ShopCategoryService;
import com.widestar.o2o.service.ShopService;
import com.widestar.o2o.util.HttpServletRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/o2o/shopadmin")
public class ShopManagementController {
  @Autowired
  private ShopService shopService;
  @Autowired
  private ShopCategoryService shopCategoryService;
  @Autowired
  private AreaService areaService;

  @RequestMapping(value = "/getshopmanagementinfo",method = RequestMethod.GET)
  @ResponseBody
  private Map<String,Object> getShopManagementInfo(HttpServletRequest request){
    Map<String,Object> modelMap=new HashMap<>();
    long shopId=HttpServletRequestUtil.getLong(request,"shopId");
    if(shopId<0){
      Object currentShopObject=request.getSession().getAttribute("currentShop");
      if(currentShopObject==null){
        modelMap.put("redirect",true);
        modelMap.put("url","/o2o/shopadmin/shoplist");
      }else{
        Shop currentShop=(Shop) currentShopObject;
        modelMap.put("redirect",false);
        modelMap.put("shopId",currentShop.getShopId());
      }
    }else{
      Shop currentShop=new Shop();
      currentShop.setShopId(shopId);
      request.getSession().setAttribute("currentShop",currentShop);
      modelMap.put("redirect",false);
    }
    return modelMap;
  }

  @RequestMapping(value = "/getshoplist",method = RequestMethod.GET)
  @ResponseBody
  public Map<String,Object> getShopList(HttpServletRequest request){
    Map<String,Object> modelMap= Maps.newHashMap();
    PersonInfo user=new PersonInfo();
    user.setUserId(1L);
    user.setName("liuzeguang");
    request.getSession().setAttribute("user",user);
    user=(PersonInfo) request.getSession().getAttribute("user");
    try{
      Shop shopCondition=new Shop();
      shopCondition.setOwner(user);
      ShopExecution se=shopService.getShopList(shopCondition,0,100);
      modelMap.put("user",user);
      modelMap.put("shopList",se.getShopList());
      modelMap.put("success",true);
    }catch(Exception e){
      modelMap.put("success",false);
      modelMap.put("errMsg",e.getMessage());
    }
    return modelMap;
  }

  @RequestMapping(value = "/getshopbyid", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getShopById(HttpServletRequest request) {
    Map<String, Object> modelMap = new HashMap<>();
    Long shopId = HttpServletRequestUtil.getLong(request, "shopId");
    if (shopId < 0) {
      modelMap.put("success", false);
      modelMap.put("errMsg", "empty shopId");
    } else {
      try {
        Shop shop = shopService.getShopById(shopId);
        List<Area> areaList = areaService.getAreaList();
        modelMap.put("success", true);
        modelMap.put("shop", shop);
        modelMap.put("areaList", areaList);
      } catch (Exception e) {
        modelMap.put("success", false);
        modelMap.put("errMsg", e.getMessage());
        return modelMap;
      }
    }
    return modelMap;
  }

  @RequestMapping(value = "/modifyshop", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, Object> modifyShop(HttpServletRequest request) {
    //获取店铺信息
    Map<String, Object> modelMap = new HashMap<>();
    String shopStr = HttpServletRequestUtil.getString(request, "shopStr");
    Shop shop;
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      shop = objectMapper.readValue(shopStr, Shop.class);
    } catch (Exception e) {
      modelMap.put("success", false);
      modelMap.put("errMsg", e.getMessage());
      return modelMap;
    }
    CommonsMultipartFile shopImg = null;
    CommonsMultipartResolver commonsMultipartResolver = new
        CommonsMultipartResolver(request.getSession().getServletContext());
    if (commonsMultipartResolver.isMultipart(request)) {
      MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest)
          request;
      shopImg = (CommonsMultipartFile) multipartHttpServletRequest.getFile("shopImg");
    }
    //修改店铺
    if (shop != null && shop.getShopId() != null) {
      ShopExecution se;
      try {
        if (shopImg != null) {
          se = shopService.modifyShop(shop, shopImg.getInputStream(), shopImg.getOriginalFilename());
        } else {
          se = shopService.modifyShop(shop, null, null);
        }
        if (se.getState() == ShopStateEnum.SUCCESS.getState()) {
          modelMap.put("success", true);
        } else {
          modelMap.put("success", false);
          modelMap.put("errMsg", se.getStateInfo());
        }
      } catch (IOException e) {
        modelMap.put("success", false);
        modelMap.put("errMsg", e.getMessage());
      } catch (ShopOperationException e) {
        modelMap.put("success", false);
        modelMap.put("errMsg", e.getMessage());
      }
    } else {
      modelMap.put("success", false);
      modelMap.put("errMsg", "请输入店铺id");
    }
    return modelMap;
  }

  @RequestMapping(value = "/getshopinitinfo", method = RequestMethod.GET)
  @ResponseBody
  private Map<String, Object> getshopInitInfo() {
    Map<String, Object> modelMap = new HashMap<>();
    List<ShopCategory> shopCategoryList;
    List<Area> areaList;
    try {
      shopCategoryList = shopCategoryService.getShopCategoryList(new ShopCategory());
      areaList = areaService.getAreaList();
      modelMap.put("shopCategoryList", shopCategoryList);
      modelMap.put("areaList", areaList);
      modelMap.put("success", true);
    } catch (Exception e) {
      modelMap.put("success", false);
      modelMap.put("errMsg", e.getMessage());
    }
    return modelMap;
  }

  @RequestMapping(value = "/registershop", method = RequestMethod.POST)
  @ResponseBody
  private Map<String, Object> registerShop(HttpServletRequest request) {
    Map<String, Object> modelMap = new HashMap<>();
    String shopStr = HttpServletRequestUtil.getString(request, "shopStr");
    ObjectMapper mapper = new ObjectMapper();
    Shop shop;
    try {
      shop = mapper.readValue(shopStr, Shop.class);
    } catch (IOException e) {
      modelMap.put("success", false);
      modelMap.put("errMsg:", e.getMessage());
      return modelMap;
    }
    CommonsMultipartFile shopImg;
    CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver(
        request.getSession().getServletContext()
    );
    if (commonsMultipartResolver.isMultipart(request)) {
      MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest)
          request;
      shopImg = (CommonsMultipartFile) multipartHttpServletRequest.getFile("shopImg");
    } else {
      modelMap.put("success", false);
      modelMap.put("errMsg:", "上传图片不能为空");
      return modelMap;
    }
    //注册店铺
    if (shop != null && shopImg != null) {
      PersonInfo owner = (PersonInfo) request.getSession().getAttribute("user");
      shop.setOwner(owner);
      ShopExecution se = null;
      try {
        se = shopService.addShop(shop, shopImg.getInputStream(), shopImg.getOriginalFilename());
        if (se.getState() == ShopStateEnum.CHECK.getState()) {
          //
          List<Shop> shopList = (List<Shop>) request.getSession().getAttribute("shopList");
          if (shopList == null) {
            shopList = new ArrayList<>();
          }
          shopList.add(se.getShop());
          request.getSession().setAttribute("shopList", shopList);
          modelMap.put("success", true);
        } else {
          modelMap.put("success", false);
          modelMap.put("errMsg", se.getStateInfo());
        }
        return modelMap;
      } catch (IOException e) {
        modelMap.put("success", false);
        modelMap.put("errMsg", se.getStateInfo());
      }

    } else {
      modelMap.put("success", false);
      modelMap.put("errMsg:", "请输入店铺信息");
    }
    return modelMap;
  }
/*
  private static void inputStreamToFile(InputStream ins, File file) {
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(file);
      int bytesRead = 0;
      byte[] buffer = new byte[1024];
      while ((bytesRead = ins.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    } catch (Exception e) {
      throw new RuntimeException("调用inputStreamToFile产生异常：" + e.getMessage());
    } finally {
      try {
        if (os != null) {
          os.close();
        }
        if (ins != null) {
          ins.close();
        }
      } catch (IOException e) {
        throw new RuntimeException("inputStreamToFile关闭io产生异常" + e.getMessage());
      }
    }
  }*/
}
