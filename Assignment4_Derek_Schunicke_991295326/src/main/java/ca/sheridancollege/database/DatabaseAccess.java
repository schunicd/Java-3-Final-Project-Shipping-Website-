package ca.sheridancollege.database;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.beans.Item;
import ca.sheridancollege.beans.SalesOrder;
import ca.sheridancollege.beans.User;
import ca.sheridancollege.logic.LogicOperations;


@Repository
public class DatabaseAccess {
	
	@Autowired
	private NamedParameterJdbcTemplate jdbc;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	@Lazy
	private LogicOperations lo;
	
	//method to generate dummy records
	public void generateDummyItems() {
		
		//clears the table in case dummy records were generated twice
		clearItemTable();
		clearSalesOrdersTable();
		//getting the items from the logic class
		ArrayList<String> items = lo.generateItems();
		//calling methods to generate items and sales orders
		generateItems(items);
		generateSalesOrders();
		
	}

	//method to clear the item table
	public void clearItemTable() {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "DELETE FROM items";
		jdbc.update(query, parameters);
	}
	
	//method to clear the sales orders table
	public void clearSalesOrdersTable() {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "DELETE FROM salesOrders";
		jdbc.update(query, parameters);
	}
	
	//method to add a new user into the users table
	public void addUser(String userName, String password) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "INSERT INTO SEC_USER "
				+ "(userName, encryptedPassword, ENABLED)"
				+ " values (:userName, :encryptedPassword, 1)";
		
		parameters.addValue("userName", userName);
		parameters.addValue("encryptedPassword", passwordEncoder.encode(password));
		
		jdbc.update(query,  parameters);
		
	}
	
	//method to add the users role into the user role table
	public void addRole(String role, long userId) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		String query = "INSERT INTO USER_ROLE (userId, roleId)" 
				+ " values (:userId, :roleId)";
		
		//if the role is not empty and equals shipper, set the users role to shipper
		if(!role.toString().isEmpty() && role.toString().equals("SHIPPER")) {
			parameters.addValue("userId", userId);
			parameters.addValue("roleId", 1);
			jdbc.update(query, parameters);
		}
		
		//if the role is not empty and equals sales, set the users role to sales
		if(!role.toString().isEmpty() && role.toString().equals("SALES")) {
			parameters.addValue("userId", userId);
			parameters.addValue("roleId", 2);
			jdbc.update(query, parameters);
		}
		
	}

	//method to search the database for a specific user
	public User findUserAccount(String userName) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "SELECT * FROM SEC_USER WHERE userName = :userName";
		parameters.addValue("userName", userName);

		//storing the user to return the users information
		ArrayList<User> users = (ArrayList<User>)jdbc.query(query, parameters,
				new BeanPropertyRowMapper<User>(User.class));
		
		//if a matching user is found, return the users information
		if(users.size() > 0) {
			return users.get(0);
		}
		//if a matching user is NOT found, return null
		else
			return null;
		
	}
	
	//method to search for the users role based off of the users id
	public List<String> getRolesById(long userId){
		
		ArrayList<String> roles = new ArrayList<String>();
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "SELECT USER_ROLE.userId, SEC_ROLE.roleName "
				+ "FROM USER_ROLE, SEC_ROLE WHERE USER_ROLE.roleId = SEC_ROLE.roleId"
				+ " AND userId = :userId";
		
		parameters.addValue("userId", userId);
		
		/*get user ID and role name where the user ID and the user role matches 
		**between user role and security role tables
		*/
		List<Map<String, Object>> rows = jdbc.queryForList(query, parameters);
		
		//create a list with each of the users roles to return
		for(Map<String, Object> row : rows){
			
			roles.add((String)row.get("roleName"));
			
		}
		
		return roles;
		
	}

	//method to get all items from the items database
	public ArrayList<Item> getItems() {
		
		String query = ("SELECT * FROM items");
		
		ArrayList<Item> item = (ArrayList<Item>)
				jdbc.query(query, new HashMap<String, Object>(),
				new BeanPropertyRowMapper<Item>(Item.class));
		
		return item;
		
	}
	
	//method to search the items database for an item
	public ArrayList<Item> searchItems(Item search){
				
		//calling logic operations to evaluate how to search for the item
		ArrayList<Item> orders = (ArrayList<Item>)
				jdbc.query(lo.getItemSearch(search), new HashMap<String, Object>(),
				new BeanPropertyRowMapper<Item>(Item.class));
		
		//if items are found, return the items
		if(orders.size() > 0)
			return orders;
		
		//if nothing is found return null
		else
			return null;
		
	}
	
	//method to get all search orders from the sales orders database
	public ArrayList<SalesOrder> getSalesOrders(){
		
		String query = ("SELECT * FROM salesOrders");
		
		ArrayList<SalesOrder> orders = (ArrayList<SalesOrder>)
				jdbc.query(query, new HashMap<String, Object>(),
						new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		return orders;
		
	}
	
	//method to add an item to a sales order
	public void addToSalesOrder(SalesOrder order) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		String query = "INSERT INTO salesOrders SET orderId = :orderId, "
				+ "custName = :custName, custEmail = :custEmail, salesName = :salesName, "
				+ "item = :item, weight = :weight, itemQty = :itemQty, price = :price, "
				+ "shipDate = :shipDate";

		//adding all values to the parameters for adding new item to sales order
		parameters.addValue("orderId", order.getOrderId());
		parameters.addValue("custName", order.getCustName());
		parameters.addValue("custEmail", order.getCustEmail());
		parameters.addValue("salesName", order.getSalesName());
		parameters.addValue("item", order.getItem());
		parameters.addValue("itemQty", order.getItemQty());
		parameters.addValue("price", order.getPrice());
		parameters.addValue("weight", order.getWeight());
		parameters.addValue("shipDate", order.getShipDate());
		
		jdbc.update(query, parameters);
		
	}

	//method to search sales orders
	public ArrayList<SalesOrder> searchOrders(SalesOrder search){
		
		String query;
		
		//if no order id is entered use this query to search
		if(search.getOrderId() != -1)
			query = "SELECT * FROM salesOrders WHERE orderId LIKE '" + search.getOrderId()
					+ "' AND custName LIKE '%" + search.getCustName() 
					+ "%' AND salesName LIKE '%" + search.getSalesName() + "%'";
		//if an order id is entered in the search window, use this query to search
		else
			query = "SELECT * FROM salesOrders WHERE orderId LIKE '%' "
					+ "AND custName LIKE '%" + search.getCustName() 
					+ "%' AND salesName LIKE '%" + search.getSalesName() + "%'";
		
		//querying database and creating a list of sales orders
		ArrayList<SalesOrder> orders = (ArrayList<SalesOrder>)
				jdbc.query(query, new HashMap<String, Object>(),
				new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		//if orders are found, return the orders
		if(orders.size() > 0)
			return orders;
		
		//if no orders are found, return null
		else
			return null;
		
	}

	//method to search the items database for an item based off the items description
	public Item getItemByDesc(String item) {
		
		String query = ("SELECT * FROM items WHERE item LIKE :item");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("item", item);
		
		//searching database for item and creating a list with any item found
		ArrayList<Item> i = (ArrayList<Item>)jdbc.query(query, parameters,
				new BeanPropertyRowMapper<Item>(Item.class));
		
		//if an item is found, return the item
		if(i.size() > 0)
			return i.get(0);
		
		//if no item is found, return null
		else
			return null;
		
	}

	//method to search the items database for an item based off the items id
	public Item getItemById(int id) {
		
		String query = ("SELECT * FROM items WHERE id = :id");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("id", id);
		
		//searching the database for the item and creating a list with it
		ArrayList<Item> item = (ArrayList<Item>)jdbc.query(query, parameters,
				new BeanPropertyRowMapper<Item>(Item.class));
		
		//if an item is found, return that item
		if(item.size() > 0)
			return item.get(0);
		
		//if an item is not found, return null
		else
			return null;
		
	}

	//method to search sales orders database by order ID
	public ArrayList<SalesOrder> getSalesOrderById(int orderId) {
		
		String query = ("SELECT * FROM salesOrders WHERE orderId = :orderId");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("orderId", orderId);
		
		//searching database and creating a list of orders
		ArrayList<SalesOrder> orders = (ArrayList<SalesOrder>)jdbc.query(query, parameters,
				new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		//if an order is found, return the order
		if(orders.size() > 0)
			return orders;
		
		//if no order is found, return null
		else
			return null;
		
	}

	//method to search sales orders database by item ID
	public SalesOrder getSalesOrderItemById(int id) {
		
		String query = ("SELECT * FROM salesOrders WHERE id = :id");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("id", id);
		
		//searching sales order data base and making a list of any items with a matcing item ID
		ArrayList<SalesOrder> item = (ArrayList<SalesOrder>)jdbc.query(query, parameters,
				new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		//if an item is found, return that item
		if(item.size() > 0)
			return item.get(0);
		
		//if an item is not found, return null
		else
			return null;
		
	}

	//method to add new items into the item database
	public void addNewItem(Item item) {
		
		String query = "INSERT INTO ITEMS SET item = :item, inventory = :inventory,"
				+ "price = :price, weight = :weight";
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		//adding the items values to the parameters for querying the database
		parameters.addValue("item", item.getItem());
		parameters.addValue("inventory", item.getInventory());
		parameters.addValue("price", item.getPrice());
		parameters.addValue("weight", item.getWeight());
		
		//querying the database to add the new item
		jdbc.update(query, parameters);
		
	}
	
	//method to edit an item in the item database
	public void editItem(Item item) {
		
		String query = "UPDATE items SET item = :item, weight = :weight, "
				+ "price = :price, inventory = :inventory WHERE id = :id";
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		//adding item values to parameters for query to update item based on unique item ID
		parameters.addValue("item", item.getItem());
		parameters.addValue("weight", item.getWeight());
		parameters.addValue("price", item.getPrice());
		parameters.addValue("inventory", item.getInventory());
		parameters.addValue("id", item.getId());

		//querying database to edit item
		jdbc.update(query, parameters);
		
	}

	//method to edit a sales order in the sales order database
	public void editSalesOrder(SalesOrder order) {
		
		String query = "UPDATE salesOrders SET item = :item, "
				+ "itemQty = :itemQty, shipDate = :shipDate WHERE id = :id";
		MapSqlParameterSource itemDetails = new MapSqlParameterSource();
		MapSqlParameterSource custSalesDetails = new MapSqlParameterSource();
		
		//adding values of the item to the parameters for querying the database
		itemDetails.addValue("id", order.getId());
		itemDetails.addValue("item", order.getItem());
		itemDetails.addValue("itemQty", order.getItemQty());
		itemDetails.addValue("weight", order.getWeight());
		itemDetails.addValue("shipDate", order.getShipDate());
		
		//querying the database to update the specific item
		jdbc.update(query, itemDetails);
		
		//adding values of customer details to parameters for querying database
		custSalesDetails.addValue("orderId", order.getOrderId());
		custSalesDetails.addValue("custName", order.getCustName());
		custSalesDetails.addValue("custEmail", order.getCustEmail());
		custSalesDetails.addValue("salesName", order.getSalesName());
		
		//query to update all items on a sales order with new customer information
		query = "UPDATE salesOrders SET custName = :custName, custEmail = :custEmail, "
				+ "salesName = :salesName WHERE orderId = :orderId";
		
		//querying database to update all items on a customer sales order with new customer information
		jdbc.update(query, custSalesDetails);
		
	}

	//method to update item inventory in item database when an item is sold
	public void purchaseItem(SalesOrder item) {
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "UPDATE items SET inventory = :inventory WHERE id = :id";
		
		//subtracting sold items from available items and adding this to parameters 
		parameters.addValue("inventory", getItemByDesc(item.getItem()).getInventory() 
				- Integer.parseInt(item.getItemQty()));
		parameters.addValue("id", getItemByDesc(item.getItem()).getId());
		
		//querying database to update inventory quantity of a specific item
		jdbc.update(query, parameters);
		
	}

	//method to delete an item from the item database
	public void deleteItem(int id) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "DELETE FROM items WHERE id = :id";
		parameters.addValue("id", id);
		jdbc.update(query, parameters);
	}
	
	//method to delete a sales order item from the sales order database
	public void deleteSalesOrderItem(int id) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String query = "DELETE FROM salesOrders WHERE id = :id";
		parameters.addValue("id", id);
		jdbc.update(query, parameters);
	}
	
	//method to find last sales order number created in the sales order database
	public int getSalesOrderNumber() {
		
		String query = ("SELECT * FROM salesOrders WHERE orderId = (SELECT MAX(orderId) "
				+ "FROM salesOrders)");
		
		ArrayList<SalesOrder> orderId = (ArrayList<SalesOrder>)
				jdbc.query(query, new HashMap<String, Object>(),
				new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		//if there are sales orders, return the highest order ID
		if(orderId.size() > 0)
			return orderId.get(0).getOrderId();
		
		//if there are no sales orders, return zero to start order ID's from one
		else
			return (Integer) 0;
		
	}

	//method to get all sales orders with a specific date from the sales orders database
	public ArrayList<SalesOrder> getSalesOrderByDate(String shipDate) {
		
		String query = "SELECT * FROM salesOrders WHERE shipDate = :shipDate";
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("shipDate", shipDate);
		
		//searching for all sales orders with a specific date
		ArrayList<SalesOrder> orders = (ArrayList<SalesOrder>)
				jdbc.query(query, parameters,
				new BeanPropertyRowMapper<SalesOrder>(SalesOrder.class));
		
		return orders;
	}
	
	/*method for getting the lowest item id from the database
	**used for generate sales order method
	*/
	public Item getMinId() {
		String query = "SELECT * FROM items WHERE id = (SELECT MIN(id) FROM items)";
		ArrayList<Item> minId = (ArrayList<Item>)jdbc.query(query, new HashMap<String, Object>(),
				new BeanPropertyRowMapper<Item>(Item.class));
		
		return minId.get(0);
	}
	
	/*method for getting the highest item id from the database
	**used for generate sales order method
	*/
	public Item getMaxId() {
		String query = ("SELECT * FROM items WHERE id = (SELECT MAX(id) FROM items)");
		ArrayList<Item> maxId = (ArrayList<Item>)jdbc.query(query, new HashMap<String, Object>(),
				new BeanPropertyRowMapper<Item>(Item.class));
		
		return maxId.get(0);
	}
	
	//method for generating items for the dummy records
	public void generateItems(ArrayList<String> items) {
		
		String query = ("INSERT INTO items (item, weight, price, inventory)"
						+ " VALUES (:item, :weight, :price, :inventory)");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		//loop through the list of items, and generate a weight, price and quantity for each
		for(int i = 0 ; i < items.size() ; i++) {
			
			parameters.addValue("item", items.get(i));
			parameters.addValue("weight", (int)((Math.random() * (300 - 100) + 100)));
			parameters.addValue("price", String.format("%.2f", ((Math.random() * (2000 - 500) + 500))));
			parameters.addValue("inventory", (int)(Math.random() * 100));
			
			//insert item into database before generating next item
			jdbc.update(query,  parameters);
			
		}
		
	}

	//method for generating sales orders for the dummy records
	public void generateSalesOrders() {

		//starting order ID's from 0
		int orderId = 0;

		String query = ("INSERT INTO salesOrders (orderId, custName, custEmail, "
				+ "salesName, item, weight, itemQty, price, shipDate)"
				+ " VALUES (:orderId, :custName, :custEmail, :salesName, "
				+ ":item, :weight, :itemQty, :price, :shipDate)");
		
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		//creating 20 sales orders to give variety of dummy records
		for(int j = 0 ; j < 20 ; j++) {
		
			//if the sales order number is zero, start off the sales order ID's from one
			if(getSalesOrderNumber() == 0)
				orderId = 1;
			//if the sales order number is greater than zero, add one to it for a new sales order number
			else {
				++orderId;
			}
			
			//randomly generating a day from one to ten for the ship date
			int shipDate = (int)((Math.random() * 10) + 1);
			
			//randomly generate one to seven items for the order
			for(int i = 0 ; i < (int)((Math.random() * 7) + 1) ; i++) {
				
				//randomly pick an item from inventory to add to the sales order
				Item item = getItemById((int)(Math.random() * (getMaxId().getId() - 
						getMinId().getId()) + getMinId().getId()));
				
				//randomly generate a quantity for the item
				int quantity = (int)((Math.random() * 20) + 1);
				
				//adding all the items values to the parameters for querying the database
				parameters.addValue("orderId", orderId);
				parameters.addValue("custName", j);
				parameters.addValue("custEmail", j + "@sheridanCollege.ca");
				parameters.addValue("salesName", j + " Sales");
				parameters.addValue("item", item.getItem());
				parameters.addValue("weight", item.getWeight());
				parameters.addValue("itemQty", quantity);
				parameters.addValue("price", item.getPrice());
				parameters.addValue("shipDate", "2019-10-" + shipDate);
				
				jdbc.update(query,  parameters);
				
			}
			
		}
		
	}
	
}
