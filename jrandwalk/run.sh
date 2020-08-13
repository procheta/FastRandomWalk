mvn compile
if [ $# -lt 8]
	echo "Usage <edge file path> <alpha> <beta> <walk_len> <numWalk> <k> <directed/undirected> <rwalkmode>(Node2vec/Biased_Random_Walk)"	

cd target/classes/


$1
java graphwalk.RWalk $1 $2 $3 $4 $5 $6 $7 $8
