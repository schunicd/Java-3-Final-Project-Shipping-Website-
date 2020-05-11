package ca.sheridancollege.beans;
import java.io.Serializable;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
//creating sales order bean and declaring its variables
public class SalesOrder implements Serializable{
	
	private static final long serialVersionUID = 1222865946232083237L;

	int id;
	int orderId;
	String custName;
	String custEmail;
	String salesName;
	String item;
	String itemQty;
	String weight;
	String price;
	String shipDate;	
	
}
