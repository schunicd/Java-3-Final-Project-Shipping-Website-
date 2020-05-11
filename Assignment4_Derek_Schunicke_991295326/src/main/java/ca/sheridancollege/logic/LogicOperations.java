package ca.sheridancollege.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import ca.sheridancollege.beans.Item;
import ca.sheridancollege.beans.SalesOrder;
import ca.sheridancollege.database.DatabaseAccess;

@Component
public class LogicOperations {
	
	@Autowired
	@Lazy
	private DatabaseAccess da;
	//initializing array list to store items
	ArrayList<String> items = new ArrayList<String>();
	//initializing pdf, encrypted pdf and excel file writers
	File pdfDir = new File(System.getProperty("user.dir").toString() + "/ShippingManifests");
	File encryptPdfDir = new File(System.getProperty("user.dir").toString() + "/EncryptedShippingManifests");
	File excelDir = new File(System.getProperty("user.dir").toString() + "/ExcelDocs");
	
	//creating directories to store the documents in
	public void makeDirectories() {
		pdfDir.mkdir();
		encryptPdfDir.mkdir();
		excelDir.mkdir();
	}
	
	//printing to PDF
	public void printPDF(ArrayList<SalesOrder> ship, String date, int loadCap) 
			throws DocumentException, IOException {
		
		//initializing the page to write to
		Document document = new Document(PageSize.A4, -50, -50, 20, 5);
		//using the PDF writer to write to the new document at the specified location
		PdfWriter.getInstance(document, new FileOutputStream("ShippingManifests/" + date + "_ShippingManifest.pdf"));
		
		//opening the document to write to it
		document.open();
		
		//setting starting manifest number and starting array index number
		int manifestNum = 1;
		int arrayIndex = 0;
		
		do {
			
			//creating the header for the top of the page
			Chunk chunk = new Chunk("                                            "
					+ "                            Shipping Manifest #" + manifestNum + 
					" For " + date);
			
			//increasing the manifest number for each new manifest
			manifestNum++;
			//creating a table with 5 columns
			PdfPTable table = new PdfPTable(5);
			/*pass the table and shipping info to another method to write to the manifest until
			**the manifest can not take any more weight
			*/
			arrayIndex = shippingManifest(table, ship, arrayIndex, loadCap);
			//adding the header to the top of the page
			document.add(chunk);
			//adding a space beneath the header
			document.add(new Paragraph(" "));
			//adding the table to the page
			document.add(table);
			//creating a new page for a new manifest
			document.newPage();
			
		//continue making more manifests until all the items have been added to one
		}while(arrayIndex < ship.size());
		
		//close the document
		document.close();
		
		//encrypt the pdf using encryptPDF method
		encryptPDF(date);
		
	}

	//creating encrypted PDF
	private void encryptPDF(String date) throws IOException, DocumentException {
		
		//reading the un-encrypted shipping manifest that we want to encrypt
		PdfReader pdfReader = new PdfReader("shippingManifests/" +date + "_ShippingManifest.pdf");
		//using PDF stamper to apply the encryption
		PdfStamper pdfStamper 
		  = new PdfStamper(pdfReader, new FileOutputStream("EncryptedShippingManifests/" 
				  + date + "_encryptedManifest.pdf"));
		
		//setting a default password of 123 and disabling the ability to print these manifests
		pdfStamper.setEncryption(
		  "123".getBytes(),
		  "".getBytes(),
		  0,
		  PdfWriter.ENCRYPTION_AES_256
		);
		
		
		pdfStamper.close();
		pdfReader.close();
		
	}
	
	//deleting all un-encrypted manifests
	public void deleteManifests() throws IOException {
		
		//cleans directory without deleting it
		FileUtils.cleanDirectory(pdfDir);
		
		//getting the list of files in the directory
		File[] listFiles = pdfDir.listFiles();
		
		//deleting each file in the directory
		for(File file : listFiles){
			
			System.out.println("Deleting "+file.getName());
			file.delete();
		}

	}

	//printing shipping manifest
	private int shippingManifest(PdfPTable table, ArrayList<SalesOrder> ship, int index, int loadCap) {
	    //setting default values for total weight and array index
		int totalWeight = 0;
		int arrayIndex = index;
	    	
			//create the column titles for the pdf spreadsheet
	    	Stream.of("Order ID", "Customer Name", "Item Desc", "Item Qty", "Total Weight")
		      .forEach(columnTitle -> {
		        PdfPCell header = new PdfPCell();
		        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
		        header.setBorderWidth(2);
		        header.setPhrase(new Phrase(columnTitle));
		        table.addCell(header);
		    });
	    	
	    	//while we are still below the load capacity of the trailer, add more items to the load
		    for(int i = arrayIndex ; i < ship.size() ; i++) {
				table.addCell(Integer.toString(ship.get(i).getOrderId()));
			    table.addCell(ship.get(i).getCustName());
			    table.addCell(ship.get(i).getItem());
			    table.addCell(ship.get(i).getItemQty());
			    table.addCell(Integer.toString(Integer.parseInt(ship.get(i).getWeight()) * 
			    		Integer.parseInt(ship.get(i).getItemQty())));
			    arrayIndex++;
			    if(totalWeight + Integer.parseInt(ship.get(i).getWeight()) * 
			    		Integer.parseInt(ship.get(i).getItemQty()) > loadCap)
			    	break;
			    else
			    	totalWeight += Integer.parseInt(ship.get(i).getWeight()) * 
			    	Integer.parseInt(ship.get(i).getItemQty());
			    
			}
		    
		    //display total weight for this load
		    table.addCell("");
			table.addCell("");
			table.addCell("");
			table.addCell("SHIPMENT WEIGHT : ");
			table.addCell(Integer.toString(totalWeight));
		    totalWeight = 0;
			return arrayIndex;

	}

	//printing to excel spreadsheet
	public void printExcel(int salesOrder) throws IOException {
		
		//getting all items on the sales order and preparing an excel spreadsheet
		ArrayList<SalesOrder> order = new ArrayList<SalesOrder>(da.getSalesOrderById(salesOrder));
		Workbook workbook = new XSSFWorkbook();
		
		//creating the excel spreadsheet and defining the sizes of each column
		Sheet sheet = workbook.createSheet("Order");
		sheet.setColumnWidth(0, 6500);
		sheet.setColumnWidth(1, 4000);
		sheet.setColumnWidth(2, 4000);
		sheet.setColumnWidth(3, 4500);
		sheet.setColumnWidth(4, 5000);
		sheet.setColumnWidth(5, 4000);
		sheet.setColumnWidth(6, 4500);
		sheet.setColumnWidth(7, 4000);
		
		//creating rows for customer info, sales person info and order info
		Row orderId = sheet.createRow(0);
		Row custName = sheet.createRow(1);
		Row custEmail = sheet.createRow(2);
		Row salesName = sheet.createRow(3);
		Row orderInfo = sheet.createRow(6);
		
		//creating header cells for the headers of each column
		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		//setting the font size and style to write with in excel
		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontName("Arial");
		font.setFontHeightInPoints((short) 16);
		font.setBold(true);
		headerStyle.setFont(font);
		
		//creating order ID header 
		Cell headerCell = orderId.createCell(0);
		headerCell.setCellValue("Order Id");
		headerCell.setCellStyle(headerStyle);
		headerCell = orderId.createCell(1);
		headerCell.setCellValue(order.get(0).getOrderId());
		
		//creating customer name header
		headerCell = custName.createCell(0);
		headerCell.setCellValue("Customer Name");
		headerCell.setCellStyle(headerStyle);
		headerCell = custName.createCell(1);
		headerCell.setCellValue(order.get(0).getCustName());
		
		//creating customer email header
		headerCell = custEmail.createCell(0);
		headerCell.setCellValue("Customer Email");
		headerCell.setCellStyle(headerStyle);
		headerCell = custEmail.createCell(1);
		headerCell.setCellValue(order.get(0).getCustEmail());
		
		//creating sales person name header
		headerCell = salesName.createCell(0);
		headerCell.setCellValue("Sales Name");
		headerCell.setCellStyle(headerStyle);
		headerCell = salesName.createCell(1);
		headerCell.setCellValue(order.get(0).getSalesName());

		//creating item description header
		headerCell = orderInfo.createCell(0);
		headerCell.setCellValue("Item Desc.");
		headerCell.setCellStyle(headerStyle);
		
		//creating item ID header
		headerCell = orderInfo.createCell(1);
		headerCell.setCellValue("Item ID");
		headerCell.setCellStyle(headerStyle);
		
		//creating quantity header
		headerCell = orderInfo.createCell(2);
		headerCell.setCellValue("Quantity");
		headerCell.setCellStyle(headerStyle);
		
		//creating unit weight header
		headerCell = orderInfo.createCell(3);
		headerCell.setCellValue("Unit Weight");
		headerCell.setCellStyle(headerStyle);
		
		//creating total weight header
		headerCell = orderInfo.createCell(4);
		headerCell.setCellValue("Total Weight");
		headerCell.setCellStyle(headerStyle);
		
		//creating unit price header
		headerCell = orderInfo.createCell(5);
		headerCell.setCellValue("Unit Price");
		headerCell.setCellStyle(headerStyle);
		
		//creating total price header
		headerCell = orderInfo.createCell(6);
		headerCell.setCellValue("Total Price");
		headerCell.setCellStyle(headerStyle);
		
		//creating ship date header
		headerCell = orderInfo.createCell(7);
		headerCell.setCellValue("Ship Date");
		headerCell.setCellStyle(headerStyle);
		
		//creating a dollar value format for the unit price and total price display
		CellStyle dollarStyle= workbook.createCellStyle();
		DataFormat df = workbook.createDataFormat();
		dollarStyle.setDataFormat(df.getFormat("$#,#0.00"));
		
		//allowing the text to wrap if it is too long for the box
		CellStyle style = workbook.createCellStyle();
		style.setWrapText(true);
		
		//Initialing the formula for the weight and price
		String weightFormula = "";
		String priceFormula = "";
		
		//used to determine which column to write to
		int i = 7;
		for(SalesOrder o : order) {
			
			//writing the item description
			Row row = sheet.createRow(i);
			Cell cell = row.createCell(0);
			cell.setCellValue(o.getItem());
			cell.setCellStyle(style);;
			
			//writing the item id
			cell = row.createCell(1);
			cell.setCellValue(o.getId());
			cell.setCellStyle(style);
			
			//writing the item quantity
			cell = row.createCell(2);
			cell.setCellValue(Integer.parseInt(o.getItemQty()));
			cell.setCellStyle(style);
			
			//writing the individual unit weight
			cell = row.createCell(3);
			cell.setCellValue(Integer.parseInt(o.getWeight()));
			cell.setCellStyle(style);
			
			//writing the total weight of all units of this type ordered
			cell = row.createCell(4);
			cell.setCellValue(Integer.parseInt(o.getItemQty()) * Integer.parseInt(o.getWeight()));
			cell.setCellStyle(style);
			//creating the formula to calculate the total weight using the cell addresses
			weightFormula += cell.getAddress() + ":";
			
			//writing the price per unit
			cell = row.createCell(5);
			cell.setCellValue(Double.parseDouble(o.getPrice()));
			cell.setCellStyle(dollarStyle);
			
			//writing the total cost of this unit/item type
			cell = row.createCell(6);
			cell.setCellValue(Double.parseDouble(o.getPrice()) * 
					Integer.parseInt(o.getItemQty()));
			cell.setCellStyle(dollarStyle);
			//creating formula to calculate the total price of all items this customer has ordered
			priceFormula += cell.getAddress() + ":";
			
			//writing the items ship date
			cell = row.createCell(7);
			cell.setCellValue(o.getShipDate());
			cell.setCellStyle(style);
			
			i++;
		}
		
		//creating a new row
		Row totalHeaders = sheet.createRow(i);
		//adding a line below Total Price column to show combined weight of all items
		Cell totalCell = totalHeaders.createCell(6);
		totalCell.setCellValue("-----------------");
		totalCell.setCellStyle(headerStyle);
		
		//adding a line below Total Weight column to show combined cost of all items
		totalCell = totalHeaders.createCell(4);
		totalCell.setCellValue("------------------");
		totalCell.setCellStyle(headerStyle);
		
		//getting the excel formulas for weight and price
		weightFormula = weightFormula.substring(0, weightFormula.length() - 1);
		priceFormula = priceFormula.substring(0, priceFormula.length() - 1);
		
		//writing the total price to the cell using the formula cell type
		Row totals = sheet.createRow(i + 1);
		Cell totalPrice = totals.createCell(6);
		totalPrice.setCellType(CellType.FORMULA);
		totalPrice.setCellFormula("SUM(" + priceFormula + ")");
		totalPrice.setCellStyle(dollarStyle);
		
		//writing the total weight to the cell using the formula cell type
		Cell totalWeight = totals.createCell(4);
		totalWeight.setCellType(CellType.FORMULA);
		totalWeight.setCellFormula("SUM(" + weightFormula + ")");
		
		//writing the sales order to the excel document
		FileOutputStream outputStream = new FileOutputStream("ExcelDocs/customer-" 
		+ order.get(0).getCustName() + "-order-" + order.get(0).getOrderId() + ".xlsx");
		workbook.write(outputStream);
		//closing the excel document
		workbook.close();
		
	}

	//different search queries for shippers searching the items database
	public String getItemSearch(Item search) {
		
		String query = "";
		
		//if the shippers search includes an item ID, min quantity and max quantity use this query
		if(search.getId() >= 0 && search.getMinQty() != -1 && search.getMaxQty() != -1)
			query = "SELECT * FROM items WHERE id LIKE '" + search.getId()
					+ "' AND item LIKE '%" + search.getItem() + "%' AND "
					+ "inventory BETWEEN " + search.getMinQty() 
					+ " AND " + search.getMaxQty();
		
		//if the shippers search includes an item ID but no min or max quantity, use this query
		else if(search.getId() >= 0 && search.getMinQty() == -1 && search.getMaxQty() == -1)
			query = "SELECT * FROM items WHERE id LIKE '" + search.getId()
				+ "' AND item LIKE '%" + search.getItem() + "%'";
		
		//if the shippers search includes an item ID and ONLY max quantity, use this query
		else if(search.getId() >= 0 && search.getMinQty() == -1 && search.getMaxQty() != -1)
			query = "SELECT * FROM items WHERE id LIKE '" + search.getId()
				+ "' AND item LIKE '%" + search.getItem() + "%' AND "
				+ "inventory < " + search.getMaxQty();
		
		//if the shippers search includes an item ID and ONLY min quantity, use this query
		else if(search.getId() >= 0 && search.getMinQty() != -1 && search.getMaxQty() == -1)
			query = "SELECT * FROM items WHERE id LIKE '" + search.getId()
				+ "' AND item LIKE '%" + search.getItem() + "%' AND "
				+ "inventory > " + search.getMinQty();
		
		/*if the shippers search does not include an item ID but has
		**both min quantity and max quantity use this query
		*/
		else if(search.getMinQty() != -1 && search.getMaxQty() != -1)
			query = "SELECT * FROM items WHERE item LIKE '%" + search.getItem() 
				+ "%' AND " + "inventory BETWEEN " + search.getMinQty() 
				+ " AND " + search.getMaxQty();
		
		//if the shippers search does not include an item ID, min quantity and max quantity use this query
		else if(search.getMinQty() == -1 && search.getMaxQty() == -1)
			query = "SELECT * FROM items WHERE item LIKE '%" + search.getItem() + "%'";
		
		//if the shippers search does not include an item ID and max quantity use this query
		else if(search.getMinQty() == -1 && search.getMaxQty() != -1)
			query = "SELECT * FROM items WHERE item LIKE '%" + search.getItem() + "%' AND "
			+ "inventory < " + search.getMaxQty();
		
		//if the shippers search does not include an item ID and min quantity use this query
		else if(search.getMinQty() != -1 && search.getMaxQty() == -1)
			query = "SELECT * FROM items WHERE item LIKE '%" + search.getItem() + "%' AND "
			+ "inventory > " + search.getMinQty();		
	
		return query;
		
	}
	
	//generating the list of item descriptions for the items array
	public ArrayList<String> generateItems(){
		
		items.add("Midea 18 CU. FT. Top-Mount Refrigerator");
		items.add("Amana 18 CU. FT. Top-Mount Refrigerator");
		items.add("Samsung 22 CU. FT. 30\" Wide French-Door Refrigerator");
		items.add("Frigidaire 18 CU. FT. Top-Mount Refrigerator");
		items.add("Brada 18 CU. FT. Top-Mount Refrigerator");
		items.add("LG 6.4 Cu. Ft. Slide-In Electric Range");
		items.add("Whirlpool 5.0 Cu. Ft. Front-Control Freestanding Electric Range");
		items.add("Hisense 5.9 Cu. Ft. Freestanding Electric Convection Range");
		items.add("Haier 5.0 Cubic Foot Freestanding Electric Self-Cleaning Range");
		items.add("Maytag 7.1 Cu. Ft. Slide-In Dual Fuel Range with Baking Drawer");
		items.add("GE 100 Series Bar Handle Built-In Dishwasher");
		items.add("KitchenAid 24 Built-In Dishwasher");
		items.add("Bosch 49 dBA Hidden-Controls Built-in Dishwasher");
		items.add("Inglis Classic Chopper Tall-Tub Built-In Dishwasher");
		items.add("Midea ProDry System and PrintShield Finish Dishwasher");
		items.add("Amana 3.5 Cu. Ft. Chest Freezer");
		items.add("Samsung 15 Cu. Ft. Chest Freezer");
		items.add("Frigidaire 17.4 Cu. Ft. Upright Freezer");
		items.add("Brada 16 Cu. Ft. Upright Freezer");
		items.add("LG 12.8 Cu. Ft. Chest Freezer");
		items.add("Whirlpool 5.5 Cu. Ft. HE Top-Load Washer with Water Faucet");
		items.add("Hisense 4.0 Cu. Ft. Top-Load Washer with Dual Action Agitator");
		items.add("Haier 5.2 Cu. Ft. Front-Load Washer with Extra Power");
		items.add("Maytag 5.0 Cu. Ft Closet-Depth Front-Load Washer");
		items.add("GE 5.8 Cu.Ft Top-Load Smart Washer with TurboWash");
		items.add("KitchenAid 6.5 Cu. Ft. Electric Dryer with Automatic Drying Control");
		items.add("Bosch 7.4 Cu. Ft. Large-Capacity Gas Dryer");
		items.add("Inglis 7.3 Cu. Ft. Front-Load Gas Dryer with Extra Power");
		items.add("Samsung 7.4 Cu. Ft. Front-Load Gas Dryer with Intuitive Touch Controls");
		items.add("Frigidaire 7.5 Multi-Steam Electric Dryer");
		
		return items;
		
	}
	
}
