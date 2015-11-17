# fuse_for_practitioners

The business use-case is an emergency master patient index for different heatlh-care providers to exchange patient information.

An HL7 message carrying patient demographic information is received by the business process, validated, transformed and sent to the Nextgate web service to add the patient information, if needed. In this part of the usecase, the intent is to develop 3 different components to accomplish this.

Active MQ can be used to separate different aspects of this flow so as to have flexibility of hosting these different routes and also be able to scale the producers and consumers of data.

Another design pattern followed is the use of the properties file to encode URLs and other connection information as opposed to hard-coding.

Message traceability is managed by using XML. For that reason, convert to XML before dropping it on the queue.

