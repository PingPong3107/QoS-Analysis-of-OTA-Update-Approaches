package com.ota.update.messageTypes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Models the payload of an update with specific filesize and the actual data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdatePayload {
	private int fileSize;
	private byte[] message;
}
