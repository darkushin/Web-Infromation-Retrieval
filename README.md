# Web-Infromation-Retrieval

In this project a search engine for product reviews is built. This search engine allows to store reviews and search through them efficiently.
The project consists of three parts:<br>
In the first part, a basic index structure is introduced. This propused structure allows to index a relatively small amount of reviews (several thousands) and search through them efficiently.<br>
In the second part, the assumption of limited amount of reviews is ommited, and changes are made to the code of part 1, in order to be able to index millions of reviews while restricting the available RAM to 1GB. This was achieved using the external merge-sort algorithm.<br>
In the third part, a ranking mechanism of the retrieved results is added. That is, given a query, the returned results will be sorted in an order such that the must relevant results will be displayed first.

In order to keep the different parts separate, each part was implemented in a different branch.

In the following sections, detailed explanation of the different parts is given:

## Part 1
The index we built contains four files:
1. 	product_index.txt
2. 	review_index.txt
3. 	token_index.txt
4. 	tokens_inverted_index.txt
 
Next, we will explain the exact structure of each of these files and which data it contains.
 
### *product_index.txt*
This structure is an index over all product IDs, i.e. over strings. Besides holding the actual product IDs, it also allows to query for the review IDs for a particular product.
Due to the high overlap between consecutive product IDs, we decided to use a (k-1)-in-k front coding index, with k=8. In addition, since each product ID is of constant size (10), we can refrain from saving a length for each entry. Another observation about the data is that reviews for a given product are given consecutively, meaning that only the range for the review IDs should be saved for each product, instead of the entire list of IDs.
The structure is a single instance of class ProductIndex, whose structure is as follows:
int k: The value of k for which the index was generated.
String dictString: the concatenated string of all product IDs, generated with (k-1)-in-k front coding.
ArrayList<ProductInfo> data: a table in which every entry is a ProductInfo instance (an inner class defined within the index class), with each entry corresponding to a single product. Every entry holds the following fields:
short stringInfo: if the given entry is the head of the block, it holds a pointer to the first letter of the product ID in the concatenated string. Otherwise, it is the prefix length of the ID.
int reviewId: the ID of the first review in the range of reviews for the product.
short spanLength: the size of the range (e.g. reviewId of 10 and spanLength of 2 means that 10, 11, 12 are reviews for this product).


The structure of the index is illustrated in the following diagram:

![image](https://user-images.githubusercontent.com/61732335/130988691-54a696f5-3a3a-493d-a0d2-7da7576b47b6.png)


### *review_index.txt*
This file holds the review index, which retains all relevant information about each single review.
The ReviewIndex class wraps a single ArrayList<ReviewInfo>. Each element corresponds to a single review ID (first element is review 0, second is 1, etc.), and contains the product ID, helpfulness numerator, helpfulness denominator, review length, and score. To compress the size of each entry, we replaced the product ID with the index of the product’s location in the product index. Then, the first 4 numbers (all except the score) are encoded using Group Varint encoding. Each ReviewInfo instance contains two fields:
- byte[] *encodedInfo*: the Group Varint encoded bytes of all fields except score.
- byte *score*: review score.


![image](https://user-images.githubusercontent.com/61732335/131180200-d1403daa-cf56-40c3-a8b3-ceee026253fc.png)


### *token_index.txt*
This file contains all the information that is related to the tokens in the collection.
All tokens are stored using (k-1)-in-k Front Coding, where we used k=8 for this index.
The information in this file is generated using the *TokenIndex* class, which holds the following class members:
* int k: an integer representing the value that was used for the (k-1)-in-k Front Coding of all the tokens in the collection.
* String dictString: the concatenated string of all tokens in the collection that was produced using the (k-1)-in-k Front Coding method.
* int numTokens: an integer representing the total number of tokens in the collection (including repetitions).
* ArrayList<TokenInfo> data: an ArrayList of *TokenInfo* objects, where *TokenInfo* is a private class defined within the *TokenIndex* class which stores all the information of a single token.<br>
Each entry in the ArrayList holds a *TokenInfo* object, and stores its fields in the next order: 
  * short stringInfo: if the given entry is the head of the block, it holds a pointer to the first letter of the token in the concatenated string. Otherwise, it is the common prefix length of this token and the previous token in the group.
  * short frequency: the number of times the token appears in different reviews of the collection, i.e. the number of reviews containing this token, without repetitions.
  * short collectionFrequency: the frequency of the token within the collection, i.e. the number of times the token appears in all reviews, including repetitions.
  * short length: the length of the token.
  *int invertedIndexPtr: an integer representing the pointer to the inverted index file. The pointer points to the first byte in the file that is related to this token.

The full structure of the index is demonstrated in the next diagram:

![image](https://user-images.githubusercontent.com/61732335/131180525-25101e0c-e054-4503-81fd-111eeaeb1c52.png)

### *token_inverted_index.txt*
This file contains the inverted index of all tokens. The inverted index list of each token is a list of the form reviewId-numAppearances of the token in each review that includes the token.
In order to save space, these values are encoded using delta-encoding. The first review in every inverted index list is inserted as the full number, where for the rest of the review ids, the difference from the previous review is computed and decoded.
In order to access this file without reading all the file, the token_dict.txt file saves a pointer for every token, which points to the number of the first byte of the token’s inverted index.
To illustrate the structure of the inverted index, consider the following example:
Assume a token appears 6 times in the collection - 3 times in review number 40, twice in review number 45 and once in review 60. First, we list together all appearances of the token in the form of reviewId1-#appearances, reviewId2-#appearances, …, hence, we get the list [40, 3, 45, 2, 60 ,1]. Next, we compute the reviews difference between every two reviews, resulting in the list [40, 3, 5, 2, 15, 1].
In the last step, we convert these integers into byte representation of the numbers using delta encoding and write the bytes into the token_inverted_index.txt file.
When reading information from this file, we can directly access the relevant byte containing the inverted index of the desired token using the pointer that was stored in the token_index file.
Notes:
1. In order to avoid reading extra bytes, which are not related to the inverted index of the desired token, we read all bytes until the first byte of the next token, or until the end of the file.
2. After reading all relevant bytes, the bytes can be decoded as we know how many numbers we expect to get (token frequency * 2). This allows us to deal with extra padding that is added to every inverted index list to create full bytes.


## Part 2
In this part we no longer assume that the raw data can fit at once in main memory. Instead, we use external merge-sort to allow index creation despite the large size of the input.
The index in this part, has the same structure as the index in the first part, except that in the process of index creation, temporary files are being created. These files store the partialy information that was read upto this point in a sorted manner.
Using these sorted files, the entire dataset can be sorted and index using a second pass on the data.
At the end of the indexing process, similarly to the first part, we build an efficent index that supports data retrieval according to a given query.


## Part 3
This part implements a ranking mechanism for results. Given a query made by a user, sort and rank the returned results, from the result that best matches the query to the result that matches the query the least.

### Product Search
In the product search we want to return the most highly ranked products that match the given query in descending order.
We rank the products based on the following criteria which will be explained below:
1. Product Quality
2. Product Relevance

After calculating both parts, the final rank of the product is calculated as a convex combination of the two, i.e. productRank = *productRelevance + (1-)*productQuality. We decided to set =0.7 since for our opinion the productRelevance  should have more weight in the final rank, however this can be changed easily by setting a different value for .
#### Product Quality
As part of our final product rank, we want to decide if the product is of high or low quality. In order to do that, we check all the reviews of the given product, and calculate a normalized score for the product based on the following attributes of the reviews:
* Product score
* Review helpfulness
* Review length
* Number of reviews.
All reviews contain a score for the product, hence we want to use the score of all reviews to calculate an average score of the product. Making a simple average of all review scores is not a good solution as it treats all reviews as equal. However, we want to give different weights to reviews based on the quality of the review. We measure the quality of the review as a function of the review’s helpfulness rate and the review’s length. Our assumption is that as more people find the review as helpful, it describes the product better and the score given by this review should count more than a review with low helpfulness level. A similar rationale applies also for the length of the review - the longer the review (upto a certain length), the more details it provides.

***Helpfulness Rate***<br>
First, we want to normalize the helpfulness of the review - reviews that have a larger denominator were marked by more people and therefore probably provide a better review of the product. Hence, we want to give them a larger weight in the total weight of the product. 
To do that, for each review we calculate a normalization factor, which is defined as the quotient of the review’s denominator and MAX_DENOMINATOR, where MAX_DENOMINATOR is the maximal denominator within the reviews of the product.
The final helpfulness value is calculated as the product of the helpfulness rate (i.e. numerator / denominator) and the normalization factor.

***Length***<br>
Similarly to the helpfulness, we want to normalize the length of the review as well. To normalize the length of the review, we divide the length of the review in the length of the longest review for this product.

***Product Quality Score***<br>
Now, we can calculate the final Product Quality Score:
The final Product Quality Score is calculated as a combination of the Length and Helpfulness scores calculated above, and takes into consideration also the number of reviews by multiplying the expected score in log(numReviews+1). 
To sum up, the final Product Quality Score is calculated using the formula: ((1/2 * normalizedLength + 1/2 * normalizedHelpfulness) * reviewScore))*log(numReviews + 1).

#### Product Relevance
In the second part of our final product rank, we want to decide how relevant the product is to the given query.
To do that, we first find all highest ranked reviews for the given query, using the vectorSpaceSearch function, implemented in this exercise.
Next, similarly to the normalization done in the previous part, we calculate in the same manner the weight that should be given to every review, only that this time we don’t use the product’s score given in the review. (Instead, the weights are scaled by a constant, giving it a similar magnitude to ProductQuality.)

In addition to normalizing the weight given to every review, in this part we address the significance of the order of the reviews returned by the vector space search - we give more weight to reviews that were ranked higher in the vector by multiplying by the inverse of the rank, i.e. reviews that were ranked 2nd and 10th in the vector space will have their score (from the previous paragraph) multiplied by ½ and 1/10 respectively. These weights are added for each product, resulting in the relevance score.


Overall, our ranking algorithm is given in the following Pseudo-Code:
1. Find all highest ranked reviews for the given query using VectorSpaceSearch()
2. From these reviews, get all product ids
3. For each product:<br>
  a. Calculate Product Quality<br>
  b. Calculate Product Relevance<br>
  c. Calculate total product rank using the formula:<br>
      `alpha * productRelevance + (1 - alpha) * productQuality`
4. Return the k highest ranking product

