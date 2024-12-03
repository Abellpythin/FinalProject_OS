# Final Project: Designing a Simple File System

## Purpose

This project will allow you to apply concepts covered during the semester and put them into practice by synthesizing new material. Specifically, the focus will be on mechanisms for allocating and accounting system resources.   This project will also give you a chance to,

1. Continue to develop your skills and ability to work on a team.
2. Enhance your communication skills by presenting your work to your peers.
3. Apply and implement concepts covered throughout the semester.  

## Introduction

Throughout the semester, we have seen in a number of places that sharing resources requires an accounting mechanism that keeps track of the resources handed out to various processes.  Examples of this include page replacement algorithms and process scheduling algorithms.  This project will revisit this concept in the context of a file system.  The accounting mechanism for a file system keeps track of the disk blocks that are available for use in holding file data.   In addition to the accounting mechanism, there is an algorithm for selecting the disk blocks that should be allocated when a request to write or store information to a disk is made.

This project involves designing a simple flat direct-access file system. The objective is to focus on the accounting mechanism rather than the full design of the file system; however, you are responsible for understanding this particular design and the implementation provided.


## File Concept

A file is a logical storage unit abstracted by an operating system from the physical properties of the storage device. A file is the smallest logical storage unit available on a secondary storage system. Files have attributes associated with them that provide metadata about the particular file. Different operating systems organize and utilize different attributes but usually consist of the following list.

- Name - A human-readable name is used to reference the file.
- Identifier - A unique tag that is used to reference the file within the file system.
- Type - Used by operating systems that support different file types.
- Location - A pointer that identifies the device and location of the file on the device.
- Size - The file's current size in bytes or other units such as blocks.
- Protection - The access-control information that determines users and processes that can read, write, or execute the file.
- Timestamp and user identification - Tracks creation, modification, and last access time. These attributes useful for additional file protection.

This project will deal only with **name**, **location**, and **size**.

## File System Structure

File systems utilize the same strategy that was used to avoid external fragamentation in main memory. Files are stored in blocks of contiguous bytes. The blocks are powers of 2,  usually 512 or 1024 bytes. Some systems allocate larger blocks; however, the larger the block, the greater the internal fragmentation. There are reasons for allocating larger blocks for specific applications, as that will yield improvement in transferring data from the device to memory. Some applications benefit from tuning the block size. For this project, blocks are set to 512 (2<sup>9</sup>) bytes in size.

![](images/fileStorage.png)
<div style="text-align:center; font-style: italic;">Fig. 1 - File allocation using blocks</div>

*Fig. 1* above shows the file structure used to store files on secondary storage.  Files may be allocated contiguous blocks, but this is not necessary.   Free blocks are allocating based on some algorithm.  Depending on the algorithm and the free blocks available, a file may have blocks that are not contiguously allocated as shown for **FILE D**  and **FILE E** in *Fig. 1*.


This project's file system structure, shown in *Fig. 2*, consists of a **free block** list,  an **inode** list, and a list of **data blocks**. 

![](images/fileSystemRep.png)
<div style="text-align:center; font-style: italic;">Fig. 2 - File System file structure.</div>

The file system begins with an array of bits representing the free block list.   The free block list uses a bit to represent the state of a data block. Each data block has a corresponding bit in the free block list that can take on a value of `1` or `0`. A `1` value means that a block is currently allocated to a file. A `0` value means that a block is currently free to be used by any file. The bits are grouped eight (8) at a time into bytes. There are 16,384 data blocks in the file system and, thus, 16,384 bits in the free block list. Within the free block list, there is a 1-to-1 mapping between a bit and its corresponding data block. For example, bit 2 corresponds to data block 2. To extract a bit in the free list, index the block it represents first.

![](images/freeBlockList.png)
<div style="text-align:center; font-style: italic;">Fig. 3 - Free block list.</div>

*Fig. 3* depicts the free block list. To find the status of data block 17, we know it is found in the free block list bit 17. Since the bits are grouped into bytes, we know that byte 0  holds bits 0-7, and byte 1 holds bits 8-15. To compute this, divide 17/8 to get the result of 2 with a remainder of 1. To find bit 17, you go to byte 1, offset  1. The assumption is that there is a byte 0, byte 1, etc., and an offset 0, 1, etc.

![](images/inode.png)
<div style="text-align:center; font-style: italic;">Fig. 4 - inode structure.</div>

Next, in the file system structure is the inode list.  *Fig. 4* shows the inode structure. The inode holds all the information about a file. There are a total of 1024 inodes in this file system. Each inode has a specific format.   The inode begins with a name field, which is 64 bytes long. This holds exactly 64 characters. Therefore, a filename can be a total of 64 characters long.   After the name field, an integer (4 bytes) stores the file size.   After that, is an array of integers where each integer represents a block pointer. A block pointer is an integer that holds the data block number.   The integer block pointers are stored by storing the 4-bytes representing the integer. You may assume that the inode is unused when the name field is null (all zeros).

![](images/dataBlocks.png)
<div style="text-align:center; font-style: italic;">Fig. 5 - Data blocks.</div>

After the list of inodes, there are the data blocks themselves. Each data block is an array of 512 bytes, as shown in *Fig. 5*.   The blocks are used to hold raw data values. Whenever a string is written to a file, it is first converted to bytes (String.getBytes()) and then written to the disk. There are a total of 16,384 disk data blocks.


## What is Provided

The following files are provided,

- `Disk.java` - Manages the physical properties of the device.
- `FreeBlockList.java` - Manages the free block list.
- `INode.java` - Manages the inodes.
- `FileSystem.java` - Provides file system functionality.
- `Main.java` - Simulates creating, opening, deleting, writing of files and reading and writing to files.

## Test File

You should design tests to check your implementation.  Since the provided project folder is a Gradle based project, you may add test classes to test your implementation.  IntelliJ makes it very easy to add JUNIT5 unit test cases.


## Task To Be Completed

Read and understand the example Java code provided with this project. You will be making modifications to the following four methods in `FileSystem.java`,

- `read()`
- `write()`
- `allocateBlocksForFile()`
- `deallocateBlocksForFile()`

You may modify `FileSystem.java` in any way you like, but the signatures of the public methods **MAY NOT** be changed. 

Also, **DO NOT** make modifications to `Disk.java`, `FreeBlockList.java`, or `INode.java`.  You may modify `Main.java` in any way you like.  


For this project, you will 

1. Design a disk block allocation algorithm.  Design a disk block allocation algorithm. Consider what it means to allocate and deallocate blocks for a file and to read and write to a file.
2. Implement the following methods in `FileSystem.java`.
	
	<ol style="list-style-type: lower-alpha;">
	<li><code>read()</code> - read from a file.
	<li><code>write()</code> - write to a file.</li>
	<li><code>allocateBlocksForFile()</code> - allocates disk blocks for a file.</li>
	<li><code>deallocateBlocksForFile()</code> - deallocates disk blocks for a file</li>
	</ol>
	
3. Prepare a 5-minute  PowerPoint presentation of your algorithms.  The presentation should include,

  <ol style="list-style-type: lower-alpha;">
  <li>A clear description of your design choice with illustrations and pictures to help elucidate your algorithms.</li>
  <li>An explanation of each method and how they work.</li>
  <li>A description of all assumptions used in this implementation.</li>
  </ol>
  
<p style="background-color:pink; padding:5px; border: 1px solid red; border-radius: 5px;">
  Be prepared to answer questions about any part of the implementation provided in this project. This requires a clear understanding of the file system as provided and its limitations and improvements.
</p>
   
Please keep your presentation to no more than 5 minutes. **You will be asked to stop once your 5 minutes are up!**.

## Submission Requirements

Since this is a group project, only one group memmer should click the <span style="background-color:green;color:white;">Use as template</span> button to copy the repo to his/her GitHub account.  Add the other members as collaborators.  This allows all member to **clone** the templated repo to their laptop. Make sure to **add** <span style="color:blue;">mrasamny@desu.edu</span> as a collaborator on the group repo. 

Each member of the group should select one of the four methods to implement.  The repo should have evidence of feature branch submission and pull requests for each member of the group.  Since you will be working on the same file, conflicts will inevitably occur.  You should become familiar with how to resolve these conflicts.  **As a precaution, DO NOT delete any local feature branches**, as you may need them should an error in conflict resolution occur on the main branch on GitHub.

You should commit your code as you work on your program.  **Your finalized product should be on your Github repo with clear evidence that each member has submitted the work on their method**.

After completing your work, submit your Github repo link on Blackboard. Your repo should show no submissions after the submission deadline. Any submissions after the deadline will result in an immediate zero (0) on the assignment. If your **program does not compile, you will receive a zero (0) on the assignment**.


## Project Rubric (80 points)

<table>
<thead>
<tr>
<td>Category</td><td>Not Available</td><td> Incomplete </td><td>Developing</td><td>Accomplished</td><td>Exemplary</td><td>Score</td>
</tr>
</thead>

<tbody>

<tr>
<th>Overall Project Implementation <br/><span style="color:red;">(20 Points)</span></th> <td>Did not submit project</td> 
<td>Project has many errors. Implementation shows evidence of confusion, and there is no conceptual understanding of what was implemented or how to implement the requirements.</td> 
<td>Project has some errors. Implementation showed signs of some confusion, with some conceptual understanding of what was implemented or how to implement the requirements.</td> 
<td>Project has no errors. Implementation showed no signs of confusion or issues with the conceptual understanding of what was implemented.</td>
<td>Project has no errors. Has a clear conceptual understanding of what was implemented. Went beyond what was provided and implemented an improved version of the project.</td>
<td></td>
</tr>
<!============== IMPLEMENTATION OF SOLUTION =====================>
<tr>
<td style="text-align:center;" colspan=7> Implementation of Solution </td>
</tr>
<! ----------------->
<tr>
<th>Implementation of <code>read</code> method  <br/><span style="color:red;">(15 Points)</span></th>
<td>Nothing provided or does not compile.</td>
<td>The method has many logical errors and does not perform the intended behavior.</td>
<td>The method has some logical errors and does not perform the intended behavior.</td>
<td>The method has one or two logical errors and mostly performs the intended behavior.</td>
<td>The method has no logical errors and performs the intended behavior.</td>
<td></td>
</tr>
<! ----------------->
<tr>
<th>Implementation of <code>write</code> method  <br/><span style="color:red;">(15 Points)</span></th>
<td>Nothing provided or does not compile.</td>
<td>The method has many logical errors and does not perform the intended behavior.</td>
<td>The method has some logical errors and does not perform the intended behavior.</td>
<td>The method has one or two logical errors and mostly performs the intended behavior.</td>
<td>The method has no logical errors and performs the intended behavior.</td>
<td></td>
</tr>
<! ----------------->
<tr>
<th>Implementation of <code>allocateBlocksForFile</code> method  <br/><span style="color:red;">(15 Points)</span></th>
<td>Nothing provided or does not compile.</td>
<td>The method has many logical errors and does not perform the intended behavior.</td>
<td>The method has some logical errors and does not perform the intended behavior.</td>
<td>The method has one or two logical errors and mostly performs the intended behavior.</td>
<td>The method has no logical errors and performs the intended behavior.</td>
<td></td>
</tr>
<! ----------------->
<tr>
<th>Implementation of <code>deallocateBlocksForFile</code> method  <br/><span style="color:red;">(15 Points)</span></th>
<td>Nothing provided or does not compile.</td>
<td>The method has many logical errors and does not perform the intended behavior.</td>
<td>The method has some logical errors and does not perform the intended behavior.</td>
<td>The method has one or two logical errors and mostly performs the intended behavior.</td>
<td>The method has no logical errors and performs the intended behavior.</td>
<td></td>
</tr>


</tbody>
</table>

## Group Project Presentation (20 Points)

The group project presentation rubric is provided in this project folder.