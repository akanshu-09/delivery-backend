package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String id;
    private Location location;
    private String name;
}
