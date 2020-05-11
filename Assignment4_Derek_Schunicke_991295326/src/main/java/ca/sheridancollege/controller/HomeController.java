package ca.sheridancollege.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itextpdf.text.*;

import java.util.ArrayList;

import ca.sheridancollege.beans.Item;

import ca.sheridancollege.beans.SalesOrder;
import ca.sheridancollege.database.DatabaseAccess;
import ca.sheridancollege.logic.LogicOperations;

@Controller
public class HomeController {

	@Autowired
	@Lazy
	//connecting to database access
	private DatabaseAccess da;
	
	@Autowired
	@Lazy
	//connecting to logical operations
	private LogicOperations lo;
	
	//mapping for home page
	@GetMapping("/")
	public String goHome() {
		//creating file folders to store excel docs, shipping manifests, and encrypted manifests
		lo.makeDirectories();		
		return "index.html";
	}
	
	//mapping to generate dummy records
	@GetMapping("/generateItems")
	public String generateItemsAndOrders() {
		//generating dummy records for items and orders
		da.generateDummyItems();
		return "index.html";
	}
	
	//mapping for registering a new user
	@GetMapping("/register")
	public String goRegistration() {
		return "register.html";
	}
	
	//registering new user, PostMapping used for security
	@PostMapping("/register")
	public String regUser(@RequestParam String name, @RequestParam String password,
			@RequestParam("roles") String role) {
		
		/*adding user to user database and retrieving the databases working key as user ID,
		**then add the user role and ID to the list of roles
		*/
		da.addUser(name, password);
		long userId = da.findUserAccount(name).getUserId();
		da.addRole(role, userId);
		
		return "redirect:/";
		
	}

	//mapping for login page
	@GetMapping("/login")
	public String goLogin(@RequestParam(required=false, defaultValue="") String logout) throws IOException {
		
		//if the user logs out, delete unencrypted manifests for safety
		if(!logout.isEmpty())
			lo.deleteManifests();
			
		return "login.html";
	}
	
	//Mapping for shipping's home page
	@GetMapping("/shippingHome")
	public String goShipHome() {
		return "/shipping/index.html";
	}
	
	//Mapping for shippers to view inventory
	@GetMapping("/viewInventory")
	public String goShipInv(HttpSession session, Model model) {
		//page variable blank allows delete or edit options to return to view inventory page
		session.setAttribute("page", "");
		//displaying items on page
		model.addAttribute("items", da.getItems());
		return "/shipping/viewInventory.html";
	}
	
	//Mapping for shippers to edit inventory items
	@GetMapping("/editItem/{id}")
	public String editItem(@PathVariable int id,@RequestParam(required=false) String page, 
			HttpSession session, Model model) {
		//adding value of page variable to the page for redirection purposes
		model.addAttribute("page", page);
		//adding specified item to the page to edit
		model.addAttribute("item", da.getItemById(id));
		return "/shipping/editInventory.html";
	}
	
	//mapping to process the request from shippers to edit an item
	@GetMapping("/modifyItem")
	public String modifyItem(HttpSession session, @ModelAttribute Item item,
			@RequestParam(required=false) String page) {
		//setting redirection to view inventory page
		String query = "redirect:/viewInventory";
		//accessing database to edit item
		da.editItem(item);
		
		//if the page variable is not blank, redirect to the search page
		if(!page.isEmpty()) 
			query = "redirect:/searchAllItems?id=" + session.getAttribute("id") + "&item=" 
				+ session.getAttribute("item") + "&minQty=" 
				+ session.getAttribute("minQty") + "&maxQty=" + session.getAttribute("maxQty");

		return query;
		
	}
	
	//mapping for shippers to delete items from database
	@GetMapping("/deleteItem/{id}")
	public String deleteItem(@PathVariable int id, HttpSession session,
			@RequestParam(required=false) String page) {
		
		//deleting item from database using unique ID
		da.deleteItem(id);
		
		//if you are deleting from the search page, return to search page
		if(page.equals("search"))
			return "redirect:/searchAllItems?id=" + session.getAttribute("id") 
			+ "&item=" + session.getAttribute("item") 
			+ "&minQty=" + session.getAttribute("minQty")
			+ "&maxQty=" + session.getAttribute("maxQty");
		
		//return to view inventory if you are deleting from inventory page
		return "redirect:/viewInventory";
	}
	
	//mapping for sales people to delete an item from an order
	@GetMapping("/deleteOrderItem/{id}")
	public String deleteSalesOrderItem(@PathVariable int id, HttpSession session,
			@RequestParam(required=false) String page) {
		
		//deleting item from database using unique ID
		da.deleteSalesOrderItem(id);
		
		//if you are deleting from the search page, return to search page
		if(page.equals("search"))
			return "redirect:/searchSalesOrders?orderId=" + session.getAttribute("orderId") 
			+ "&custName=" + session.getAttribute("custName") 
			+ "&salesName=" + session.getAttribute("salesName");
		
		//else if you are deleting from the view sales orders page, return to this page instead
		return "redirect:/viewSalesOrders";
	}
	
	//mapping for shippers to add a new item to the database
	@GetMapping("/addItem")
	public String addItem() {
		return "/shipping/addItem.html";
	}
	
	//mapping to process the shippers request to add a new item
	@GetMapping("/addNewItem")
	public String addNewItem(@RequestParam(required=false) String item, 
			@RequestParam(required=false) int quantity, 
			@RequestParam(required=false) String price, 
			@RequestParam(required=false) String weight) {
		
		//setting the values for the new item
		Item i = new Item();
		i.setItem(item);
		i.setInventory(quantity);
		i.setPrice(price);
		i.setWeight(weight);
		
		//adding the new item to the database
		da.addNewItem(i);
		
		return "redirect:/viewInventory";
		
	}

	//mapping for shippers to search for items
	@GetMapping("/searchItems")
	public String searchItem() {
		return "/shipping/searchItem.html";
	}
	
	//mapping to process shippers request to search for items
	@GetMapping("/searchAllItems")
	public String searchItems(Model model, HttpSession session,
			@RequestParam(required=false, defaultValue="-1") String id,
			@RequestParam(required=false) String item, 
			@RequestParam(required=false, defaultValue="-1") String minQty,
			@RequestParam(required=false, defaultValue="-1") String maxQty) {
		
		//creating item to search for
		Item search = new Item();
		search.setId(Integer.parseInt(id));
		search.setItem(item);
		search.setMinQty(Integer.parseInt(minQty));
		search.setMaxQty(Integer.parseInt(maxQty));
		/*saving the attributes of the item we are searching for allowing us to return
		**to the search page with the same item loaded if we decide to edit or delete the item
		**from the search page
		*/
		session.setAttribute("id", id);
		session.setAttribute("item", item);
		session.setAttribute("minQty", minQty);
		session.setAttribute("maxQty", maxQty);
		//searching the database for the items
		model.addAttribute("items", da.searchItems(search));
		
		return "/shipping/searchItem.html";
		
	}

	//mapping to page for printing shipping manifests
	@GetMapping("/print")
	public String print(Model model) {
		//displaying all sales orders
		model.addAttribute("orders", da.getSalesOrders());
		return "/shipping/printShipDoc.html";
	}

	//mapping to process the request to print a shipping manifest
	@GetMapping("/printManifest")
	public String printManifest(Model model, 
			@RequestParam(required=false, defaultValue="2019-09-01") String date, 
			@RequestParam(required=false, defaultValue="0") int loadCap) 
			throws DocumentException, IOException, URISyntaxException {
		
		//creating a list of all sales orders for a specific date
		ArrayList<SalesOrder> ship = new ArrayList<SalesOrder>(da.getSalesOrderByDate(date));
		//displaying all sales orders
		model.addAttribute("orders", da.getSalesOrders());
		
		//if there are no items to be shipped that day, make the user aware and return to printing page
		if(ship.isEmpty()) {
			model.addAttribute("failure", "Please Enter A Valid Ship Date!!");
			return "/shipping/printShipDoc.html";
		}
		
		//printing the shipping manifest to PDF
		lo.printPDF(ship, date, loadCap);
		
		//notifying the user that their shipping manifest has printed
		model.addAttribute("success", "Success!! The Shipping Manifest Has Been Printed");
		return "/shipping/printShipDoc.html";
		
	}

	//mapping for sales home page
	@GetMapping("/salesHome")
	public String goSalesHome() {
		return "/sales/index.html";
	}
	
	//mapping for sales people to view inventory
	@GetMapping("/viewSalesInventory")
	public String viewSalesInventory(Model model) {
		//displaying items on page
		model.addAttribute("items", da.getItems());
		return "/sales/viewSalesInventory.html";
	}
	
	//mapping for sales people to view sales orders
	@GetMapping("/viewSalesOrders")
	public String viewSalesItems(HttpSession session, Model model) {
		//displaying sales orders on page
		model.addAttribute("orders", da.getSalesOrders());
		return "/sales/viewSalesOrders.html";
	}

	//mapping for sales people to edit an order
	@GetMapping("/editOrder/{id}")
	public String editSalesOrder(@PathVariable int id, Model model, 
			@RequestParam(required=false) String page) {
		//adding page attribute to determine redirection
		model.addAttribute("page", page);
		//displaying items
		model.addAttribute("items", da.getItems());
		//displaying sales order ID, customer information, and sales person information
		model.addAttribute("order", da.getSalesOrderItemById(id));
		return "/sales/editSalesOrder.html";
	}
	
	//mapping to process sales persons request to edit an order
	@GetMapping("/modifyOrder")
	public String modifySalesOrder(@ModelAttribute SalesOrder order, HttpSession session,
			@RequestParam(required=false) String page) {
		//editing sales order
		da.editSalesOrder(order);
		
		//if page is not empty, return to sales page
		if(!page.isEmpty())
			return "redirect:/searchSalesOrders?orderId=" + session.getAttribute("orderId") 
			+ "&custName=" + session.getAttribute("custName") 
			+ "&salesName=" + session.getAttribute("salesName");
		
		//if page is empty return to view sales orders
		return "redirect:/viewSalesOrders";
	}

	//mapping for sales people to add a new sales order
	@GetMapping("/addOrder")
	public String addSalesOrder(Model model, Principal principal) {
		//retrieving the logged in users name and using that as salesperson name
		model.addAttribute("salesName", principal.getName());
		//creating new sales order number
		model.addAttribute("orderId", da.getSalesOrderNumber() + 1);
		//displaying items for sale
		model.addAttribute("items", da.getItems());
		return "/sales/addSalesOrder.html";
	}

	//mapping to add the specific item to the new sales order
	@GetMapping("/addSalesItem")
	public String addSalesItem(Model model, Principal principal, @RequestParam int orderId,
			 @RequestParam(required=false, defaultValue="") String custName, 
			 @RequestParam(required=false, defaultValue="") String custEmail, 
			 @RequestParam(required=false, defaultValue="") String salesName, 
			 @RequestParam(required=false, defaultValue="") String item,
			 @RequestParam(required=false, defaultValue="0") int itemQty, 
			 @RequestParam(required=false, defaultValue="2019-09-09") String shipDate) {
		
		//creating new sales order and adding each item
		SalesOrder nextItem = new SalesOrder();
		nextItem.setOrderId(orderId);
		nextItem.setCustName(custName);
		nextItem.setCustEmail(custEmail);
		nextItem.setSalesName(salesName);
		nextItem.setItem(item);
		nextItem.setItemQty(Integer.toString(itemQty));
		nextItem.setWeight(da.getItemByDesc(item).getWeight());
		nextItem.setPrice(da.getItemByDesc(item).getPrice());
		nextItem.setShipDate(shipDate);
		
		//if the sales person sells more units than are available, notify sales person
		if(Integer.parseInt(nextItem.getItemQty()) > da.getItemByDesc(item).getInventory()) {
			
			//reloading sales person information, order ID and item list
			model.addAttribute("salesName", principal.getName());
			model.addAttribute("orderId", orderId);
			model.addAttribute("items", da.getItems());
			
			//if the order ID is the same as the last item entered in the database, use this order ID
			if(nextItem.getOrderId() == da.getSalesOrderNumber())
				model.addAttribute("order", da.getSalesOrderById(da.getSalesOrderNumber()));
			
			//notify sales person of current quantity of item
			model.addAttribute("invldQty", "There are only " + da.getItemByDesc(item).getInventory()
					+ " of the item you selected left. Please select less,"
					+ " or pick a different item.");
			return "/sales/addSalesOrder.html";
			
		}
		//adding the item to the sales order
		da.addToSalesOrder(nextItem);
		//removing quantity purchased from database
		da.purchaseItem(nextItem);
		//display customer info, sales person info, order info and available items
		model.addAttribute("salesName", principal.getName());
		model.addAttribute("custName", custName);
		model.addAttribute("custEmail", custEmail);
		model.addAttribute("date", shipDate);
		model.addAttribute("orderId", orderId);
		model.addAttribute("items", da.getItems());
		model.addAttribute("order", da.getSalesOrderById(da.getSalesOrderNumber()));
		
		return "/sales/addSalesOrder.html";
	}
	
	//mapping for sales people to search sales orders
	@GetMapping("/searchSales")
	public String searchSales() {
		return "/sales/searchSalesOrders.html";
	}
	
	//mapping to process sales persons request to search sales orders
	@GetMapping("/searchSalesOrders")
	public String searchSalesOrders(@RequestParam(required=false, defaultValue="-1") int orderId, 
			@RequestParam(required=false) String custName, 
			@RequestParam(required=false) String salesName, Model model, HttpSession session) {
		
		//setting session attributes so order ID, customer name and sales name will display on reload
		session.setAttribute("orderId", orderId);
		session.setAttribute("custName", custName);
		session.setAttribute("salesName", salesName);
		
		//storing sales order to search for
		SalesOrder search = new SalesOrder();
		search.setOrderId(orderId);
		search.setCustName(custName);
		search.setSalesName(salesName);
		
		//searching database and displaying all relevant sales orders
		model.addAttribute("orders", da.searchOrders(search));
		
		return "/sales/searchSalesOrders.html";
		
	}
	
	//mapping for sales person to print customers order to excel spread sheet
	@GetMapping("/printExcel")
	public String printExcel(Model model) {
		//displaying sales orders for sales person to select from
		model.addAttribute("orders", da.getSalesOrders());
		return "/sales/printExcelDoc.html";
	}
	
	//mapping to process request to print excel spread sheet
	@GetMapping("/printExcelDoc")
	public String printExcelDoc(Model model, 
			@RequestParam(required=false, defaultValue="0") int salesOrder) throws IOException{
		
		//displaying sales orders
		model.addAttribute("orders", da.getSalesOrders());
		
		//if sales person does not enter valid order ID notify sales person
		if(salesOrder == 0 || da.getSalesOrderById(salesOrder) == null) {
			model.addAttribute("failure", "Please Enter A Valid Order Number!!");
			return "/sales/printExcelDoc.html";
		}
		
		//print to excel
		lo.printExcel(salesOrder);
		
		//notify sales person excel document printed successfully
		model.addAttribute("success", "Success! Your Document Has Been Printed.");
		return "/sales/printExcelDoc.html";
	}

	//mapping if a username/password is bad or someone tries to access a page they are not aloud to
	@GetMapping("/access-denied")
	public String goAccessDenied() {
		return "/error/access-denied.html";
	}

}
