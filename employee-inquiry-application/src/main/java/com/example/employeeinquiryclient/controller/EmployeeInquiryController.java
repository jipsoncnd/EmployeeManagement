package com.example.employeeinquiryclient.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.example.employeeinquiryclient.model.EmployeeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

	@Controller
	public class EmployeeInquiryController {
	    
		@Autowired
	    private RestTemplate restTemplate;
		
		@Autowired
		AccessTokenBean accessTokenBean;
	    
		
		@RequestMapping("/")
		public String welcome(){
			return "welcome";
		}

		
		
		@RequestMapping("/index")
		public String welcome(Map<String, Object> model, @RequestParam(value="code", required=false) String code) throws JsonProcessingException, IOException{
			if(accessTokenBean.getAccess_token() == null) {
				String bearerToken = getAccessToken(code);
				accessTokenBean.setAccess_token(bearerToken);
			}
			return "index";
		}
		
		
		@GetMapping(value = "/empDetails/{id}")
		public String getEmployeeDetails(Map<String, Object> model,@PathVariable Long id) {
			String bearerToken = "Bearer " + accessTokenBean.getAccess_token();

			String url = "https://zuul-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/dashboard-feign/" + id;
//			             "https://zuul-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/"
	        HttpHeaders requestHeaders = new HttpHeaders();
	        requestHeaders.add("Authorization", bearerToken);
	        HttpEntity<?> httpEntity = new HttpEntity<Object>(requestHeaders);
	        System.out.println("requestHeaders----------------->getEmployeeDetails:" + requestHeaders.toString());
	        ResponseEntity<EmployeeInfo> emp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, EmployeeInfo.class);
	        System.out.println("requestHeaders----------------->getEmployeeDetails-----111111111");
	        model.put("employee", emp.getBody());
	        System.out.println("requestHeaders----------------->getEmployeeDetails-----222222222");
	        
	        Long managerId = emp.getBody().getManager();
	        String url1 = "https://zuul-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/dashboard-feign/" + managerId;
	        HttpHeaders requestHeaders1 = new HttpHeaders();
	        requestHeaders1.add("Authorization", bearerToken);
	        HttpEntity<?> httpEntity1 = new HttpEntity<Object>(requestHeaders1);
	        System.out.println("requestHeaders:----getEmployeeDetails-----333333333333" + requestHeaders1.toString());
	        ResponseEntity<EmployeeInfo> manager = restTemplate.exchange(url1, HttpMethod.GET, httpEntity1, EmployeeInfo.class);
	        model.put("manager", manager.getBody());
	        return "empDetails";
		}
		
		
		@RequestMapping("/empHierarchy/{id}")
		public String getEmployeeHierarchy(Map<String, Object> model,@PathVariable Long id) {
				String bearerToken = "Bearer " + accessTokenBean.getAccess_token();
				String url = "https://zuul-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/dashboard-feign/" + id;
		        HttpHeaders requestHeaders = new HttpHeaders();
		        requestHeaders.add("Authorization", bearerToken);
		        HttpEntity<?> httpEntity = new HttpEntity<Object>(requestHeaders);
		        System.out.println("requestHeaders:-----getEmployeeHierarchy" + requestHeaders.toString());
		        ResponseEntity<EmployeeInfo> manager = restTemplate.exchange(url, HttpMethod.GET, httpEntity, EmployeeInfo.class);
		        model.put("manager", manager.getBody());
		        try {
					String url1 = "https://zuul-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/dashboard-feign/manager/" + id;
			        HttpHeaders requestHeaders1 = new HttpHeaders();
			        requestHeaders1.add("Authorization", bearerToken);
			        HttpEntity<?> httpEntity1 = new HttpEntity<Object>(requestHeaders1);
			        System.out.println("requestHeaders:getEmployeeHierarchy--dashboard-feign manager" + requestHeaders1.toString());
			        ResponseEntity<Collection> emp = restTemplate.exchange(url1, HttpMethod.GET, httpEntity1, Collection.class);
			        model.put("employeeList", emp.getBody());
	        } catch(Exception e) {
	        	if(e.getMessage().equals("403 Forbidden"))
	        		throw new ForbiddenException();
	        }
	        return "empHierarchy";
		}
		
		@RequestMapping("/logout")
		public ModelAndView logout() {
			if(accessTokenBean.getAccess_token() != null) {
				accessTokenBean.setAccess_token(null);
				System.out.println("Resetting Access Token -----");
				System.out.println("Logging out  -----");
			}
			
			String logoutUrl = "https://employee-auth-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/logout";
			return new ModelAndView("redirect:" + logoutUrl);
		}
		
		
		public String getAccessToken(String code) throws JsonProcessingException, IOException {
			ResponseEntity<String> response = null;
			System.out.println("Authorization Code------" + code);
			RestTemplate restTemplate = new RestTemplate();
			
			String credentials = "employeeapp:pass";
			String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));
			String basicAuthHeader = "Basic " + encodedCredentials;
			
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.add("Authorization", basicAuthHeader);
	
			HttpEntity<String> request = new HttpEntity<String>(headers);
	
			String access_token_url = "https://employee-auth-service-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/oauth/token";
			access_token_url += "?code=" + code;
			access_token_url += "&grant_type=authorization_code";
			//access_token_url += "&redirect_uri=http://localhost:8082/index";
			access_token_url += "&redirect_uri=https://employee-inquiry-application-dev.apps.nonprod.apprd01.uk.hclcnlabs.com/index";
			
			response = restTemplate.exchange(access_token_url, HttpMethod.POST, request, String.class);
			String tokenResponse = response.getBody();
			System.out.println("Access Token Response ---------" + tokenResponse);
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(tokenResponse);
			String accessToken = node.path("access_token").asText();
			
			return accessToken;
		}
	}
